package com.tmsproject.order.service;

import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.cargo.entity.CargoType;
import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * TransportOrderService에 대한 통합 테스트.
 *
 * @SpringBootTest는 실제 애플리케이션과 동일하게 전체 Spring Context(모든 Bean, 실제 DB 연결)를 띄운다.
 * 그래서 Mock 없이 실제 TransportOrderService -> Repository -> MariaDB까지 전부 실제로 동작하는지 검증할 수 있다.
 * DB까지 붙이는 통합테스트에 속한다.
 *
 * 참고
 * - @DataJpaTest : JPA 관련 Bean만 띄우고 기본적으로 인메모리 DB를 써서 훨씬 빠름 (Repository만)
 * - @WebMvcTest : Controller만 사용
 * - 테스트가 많아지고 실행 속도가 느려지면 고려
 *
 * @Transactional을 테스트 클래스에 붙이면, 각 테스트 메서드가 끝날 때마다 자동으로 롤백된다.
 * 그래서 이 테스트를 몇 번을 실행해도 DB에 테스트용 더미 데이터가 쌓이지 않는다.
 */
@SpringBootTest
@Transactional
public class TransportOrderServiceTest {

    @Autowired
    private TransportOrderService transportOrderService;

    @Test
    @DisplayName("운송주문을 등록하면 화물이 함께 저장되고 주문번호가 자동 생성된다.")
    void registerOrder_saveCargoTogether_andGeneratesOrderNo() {
        // given: 화물 1개가 포함된 운송주문을 준비한다
        TransportOrder order = TransportOrder.builder()
                .originAddress("서울시 강남구 테헤란로 123")
                .originLat(37.5012)
                .originLng(127.0396)
                .destAddress("부산시 해운대구 센텀중앙로 45")
                .destLat(35.1691)
                .destLng(129.1306)
                .requestedPickupAt(LocalDateTime.now().plusDays(1))
                .requestedDeliveryAt(LocalDateTime.now().plusDays(2))
                .build();

        Cargo cargo = Cargo.builder()
                .widthCm(120)
                .lengthCm(100)
                .heightCm(150)
                .weightKg(BigDecimal.valueOf(350.5))
                .quantity(2)
                .cargoType(CargoType.GENERAL)
                .build();
        order.addCargo(cargo);

        // when: 등록한다
        TransportOrder saved = transportOrderService.registerOrder(order);

        // then: 저장 결과를 검증한다
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNo()).startsWith("TO-");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(saved.getCargoList()).hasSize(1);
        assertThat(saved.getCargoList().getFirst().getWeightKg())
                .isEqualByComparingTo(BigDecimal.valueOf(350.5));
    }

    @Test
    @DisplayName("존재하지 않는 주문을 조회하면 ORDER_NOT_FOUND 예외가 발생한다")
    void getOrder_throwsException_whenOrderNotFound() {
        // given: 절대 존재할 수 없는 ID를 하나 정한다
        Long notExistOrderId = 999_999L;

        // when & then: 예외 타입과 그 안의 ErrorCode까지 함께 검증한다
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> transportOrderService.getOrder(notExistOrderId))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("상태별로 주문 목록을 조회할 수 있다")
    void getOrders_filtersByStatus() {
        // given: RECEIVED 상태인 주문을 하나 등록해둔다 (등록 직후엔 항상 RECEIVED)
        TransportOrder order = TransportOrder.builder()
                .originAddress("경기도 성남시 분당구")
                .destAddress("인천시 남동구")
                .build();
        order.addCargo(Cargo.builder()
                .widthCm(50)
                .lengthCm(50)
                .heightCm(50)
                .weightKg(BigDecimal.valueOf(10))
                .quantity(1)
                .cargoType(CargoType.GENERAL)
                .build());
        transportOrderService.registerOrder(order);

        // when: RECEIVED 상태로 필터링해서 조회한다
        Page<TransportOrder> receivedOrders =
                transportOrderService.getOrders(OrderStatus.RECEIVED, PageRequest.of(0, 20));

        // then: 결과가 비어있지 않고, 전부 RECEIVED 상태여야 한다
        assertThat(receivedOrders.getContent()).isNotEmpty();
        assertThat(receivedOrders.getContent())
                .allMatch(o -> o.getStatus() == OrderStatus.RECEIVED);
    }

}
