package com.tmsproject.dispatch.service;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.order.repository.TransportOrderRepository;
import com.tmsproject.support.TestFixtures;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.repository.VehicleRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tmsproject.support.TestFixtures.order;
import static com.tmsproject.support.TestFixtures.vehicle1Ton;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 낙관적 락(@Version) 동시성 통합 테스트.
 *
 * 왜 @Transactional을 붙이지 않는가:
 * - 테스트 클래스에 @Transactional을 붙이면 테스트 전체가 하나의 트랜잭션으로 묶여 커밋되지 않는다.
 * - 동시성 문제는 "서로 다른 트랜잭션이 각각 커밋할 때" 생기므로, 실제로 커밋되는 환경이 필요하다.
 * - 대신 테스트 데이터를 직접 만들고 @AfterEach에서 직접 지운다.
 *
 * 실행 조건: local 프로파일의 Docker MariaDB(3308)가 떠 있어야 한다.
 */
@SpringBootTest
class DispatchConcurrencyTest {

    @Autowired private DispatchService dispatchService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private TransportOrderRepository transportOrderRepository;
    @Autowired private TransactionTemplate transactionTemplate; // "이 블록만 트랜잭션으로 실행해라"를 코드로 지정하는 도구 (@Transactional 대신)
    @Autowired private JdbcTemplate jdbcTemplate; // JPA의 캐시나 지연로딩과 무관하게 DB의 실제 상태로 하기 위해서 JDBCTemplate을 사용

    private Long vehicleId;
    private Long orderId1;
    private Long orderId2;

    // 매번 새 차량 1대와 주문 2건을 만든다.
    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime() % 1_000_000_000L;
        vehicleId = vehicleRepository.save(vehicle1Ton("T-" + suffix)).getId();
        orderId1 = saveOrder("TEST-" + suffix + "-1");
        orderId2 = saveOrder("TEST-" + suffix + "-2");
    }

    // 테스트가 끝나고 데이터를 삭제한다. 삭제 순서 중요
    @AfterEach
    void tearDown() {
        // 외래키 순서대로 삭제: 배차 → 화물 → 주문 → 차량
        jdbcTemplate.update("DELETE FROM dispatch WHERE vehicle_id = ?", vehicleId);
        jdbcTemplate.update("DELETE FROM cargo WHERE transport_order_id IN (?, ?)", orderId1, orderId2);
        jdbcTemplate.update("DELETE FROM transport_order WHERE id IN (?, ?)", orderId1, orderId2);
        jdbcTemplate.update("DELETE FROM vehicle WHERE id = ?", vehicleId);
    }

    // @Version이 동작하는지 자체를 검증
    // 시간 →
    //A: [조회 v0] ─────────────────────────────── [저장 시도 v0] ❌ 충돌
    //B:            [조회 v0 → ASSIGNED → 커밋 v1] ✅
    @Test
    @DisplayName("[결정적] 다른 사용자가 먼저 변경한 차량을 오래된 버전으로 저장하면 실패한다")
    void staleVersion_save_throwsOptimisticLock() {
        // given: 사용자 A가 차량을 조회해 둔다 (version = 0)
        //        자기만의 짧은 트랜잭션을 열어 조회하고 바로 닫는다. (트랜잭션이 없기 때문)
        //        트랜잭션 밖에서 조회했으므로 이 객체는 DB와 연결이 끊긴(detached 준영속 객체) 사본이다 (이 객체의 상태를 바꿔도 DB에 자동 반영되지 않는다.)
        Vehicle staleCopy = vehicleRepository.findById(vehicleId).orElseThrow();

        // 그 사이 사용자 B가 같은 차량을 배정하고 커밋한다 (DB version: 0 → 1)
        // executeWithoutResult : 괄호 안의 코드를 트랜잭션 하나로 묶어 실행하고 커밋한다.
        // 영속상태이기 때문에 markAsAssigned()로 상태만 바꿔도 dirty checking으로 UPDATE를 보낸다.
        transactionTemplate.executeWithoutResult(status ->
                vehicleRepository.findById(vehicleId).orElseThrow().markAsAssigned());

        // when & then: A가 오래된 사본(version 0)으로 변경을 저장하려 하면 충돌로 실패해야 한다
        staleCopy.markAsAssigned();
        Assertions.assertThatThrownBy(() -> vehicleRepository.save(staleCopy)) // 준영속 객체를 저장: version을 비교해서 다르면 예외를 던져야 한다.
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    // 두 스레드가 진짜로 동시에 확정한다. (경쟁 테스트)
//    @Test
    @RepeatedTest(20)
    @DisplayName("[경쟁] 두 사용자가 같은 차량에 서로 다른 주문을 동시에 확정하면 1건만 성공한다")
    void concurrentConfirm_sameVehicle_onlyOneSucceeds() throws InterruptedException {
        // given: 두 스레드가 "준비 완료" 후 같은 신호에 동시에 출발하도록 래치를 둔다
        ExecutorService executor = Executors.newFixedThreadPool(2); // 작업 스레드 2개를 가진 스레드 풀
        CountDownLatch ready = new CountDownLatch(2);   // 두 스레드가 모두 준비될 때까지 대기
        CountDownLatch start = new CountDownLatch(1);   // 출발 신호
        CountDownLatch done = new CountDownLatch(2);    // 두 스레드가 모두 끝날 때까지 대기
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        for (Long orderId : List.of(orderId1, orderId2)) {
            executor.submit(() -> {
                ready.countDown();                                  // ① "나 준비됐어" (2 → 1 → 0)
                try {
                    start.await();                                  // ② 출발 신호까지 대기
                    dispatchService.confirmDispatch(orderId, vehicleId);  // ③ 확정 시도
                    successCount.incrementAndGet();                 // ④ 성공하면 +1
                } catch (Throwable t) {
                    failures.add(t);                                // ⑤ 실패하면 예외 보관
                } finally {
                    done.countDown();                               // ⑥ "나 끝났어" (성공·실패 상관없이)
                }
            });
        }

        // when: 두 스레드가 준비되면 동시에 출발
        ready.await();
        start.countDown();
        // 10초 안에 두 스레드가 모두 끝났는지도 검증 (멈춤·교착 상태를 명확한 메시지로 드러냄)
        boolean finished = done.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(finished).as("두 스레드가 10초 안에 끝나야 한다").isTrue();

        // then ①: 정확히 1건만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(1);

        // then ②: 실패 원인은 둘 중 하나여야 한다
        //   - 동시에 읽고 동시에 커밋 → 나중 커밋이 버전 충돌 (낙관적 락이 막은 경우)
        //   - 한쪽이 먼저 커밋한 뒤 읽음 → 이미 ASSIGNED라 상태 검증에서 실패 (기존 검증이 막은 경우)
        assertThat(failures.getFirst()).satisfiesAnyOf(
                t -> assertThat(t).isInstanceOf(OptimisticLockingFailureException.class),
                t -> assertThat(t).isInstanceOf(BusinessException.class)
                        .extracting("errorCode").isEqualTo(ErrorCode.VEHICLE_NOT_AVAILABLE)
        );

        // then ③: DB에도 이 차량의 확정 배차는 1건뿐이어야 한다
        Integer confirmedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch WHERE vehicle_id = ? AND dispatch_status = 'CONFIRMED'",
                Integer.class, vehicleId);
        assertThat(confirmedCount).isEqualTo(1);
    }

    // ---------- 테스트 도우미 ----------

    /** 주문번호를 부여해 저장하고 ID를 돌려준다 (order_no는 NOT NULL + UNIQUE) */
    private Long saveOrder(String orderNo) {
        TransportOrder order = order();
        order.assignOrderNo(orderNo);
        return transportOrderRepository.save(order).getId(); // save 한 번에 화물까지 함게 저장된다. (cascade = ALL)
    }
}
