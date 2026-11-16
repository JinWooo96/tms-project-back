package com.tmsproject.dispatch.optimization;

import com.tmsproject.dispatch.optimization.dto.AssignmentCandidate;
import com.tmsproject.dispatch.optimization.dto.OptimizationResult;
import com.tmsproject.dispatch.optimization.dto.OptimizationResult.Assignment;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * DispatchOptimizer 단위 테스트.
 *
 * 옵티마이저는 엔티티·DB·스프링을 전혀 모르도록 설계했기 때문에(v2 정리본 10.5-(3))
 * new로 직접 만들어서 숫자만 넣고 결과를 검증할 수 있다. → 스프링 컨텍스트 없이 빠르게 실행된다.
 *
 * 각 테스트는 Given(입력 준비) - When(실행) - Then(결과 검증) 구조로 작성한다.
 */
public class DispatchOptimizerTest {

    private DispatchOptimizer optimizer;

    @BeforeEach
    void setUp() {
        // 테스트마다 새 객체를 만들어서 테스트끼리 서로 영향을 주지 않게 한다.
        optimizer = new DispatchOptimizer();
    }

    @Test
    @DisplayName("후보가 하나도 없으면(모두 용량 초과) 배정 0건, 모든 주문,차량이 미배정으로 남는다")
    void noCandidates_returnsNoAssignments() {
        // given: 주문 2건, 차량 1대가 있지만 canCarry를 통과한 후보 조합이 없다.
        List<AssignmentCandidate> candidates = List.of();
        List<Long> orderIds = List.of(1L, 2L);
        List<Long> vehicleIds = List.of(10L);

        // when
        OptimizationResult result = optimizer.solve(candidates, orderIds, vehicleIds);

        // then
        assertThat(result.assignments()).isEmpty(); // 최종적으로 선택된 주문은 비어있어야 함.
        assertThat(result.unassignedOrderIds()).containsExactlyInAnyOrder(1L, 2L); // 배정받지 못한 주문 (순서는 상관없고, 모두 포함되어야 함)
        assertThat(result.unassignedVehicleIds()).containsExactly(10L); // 배정받지 못한 차량
    }

    @Test
    @DisplayName("[회귀 방지 12.1] 후보가 있으면 '아무것도 배정하지 않음'을 답으로 내지 않는다")
    void singleCandidate_isAssigned() {
        // given: 배정 가능한 조합이 딱 하나 있다. (거리 50km)
        // 거리 최소화만 목표로 두면 "배정 안 함(거리 합 0)"이 최적이 되던 버그를 막는 테스트
        List<AssignmentCandidate> candidates = List.of(candidate(1L, 10L, 50.0));

        // when
        OptimizationResult result = optimizer.solve(candidates, List.of(1L), List.of(10L));

        // then
        assertThat(result.assignments())
                .extracting(Assignment::orderId, Assignment::vehicleId) // orderId, vehicleId만 추출
                .containsExactly(tuple(1L, 10L)); // tuple: 여러 값을 하나의 묶음으로 만드는 메서드
        assertThat(result.unassignedOrderIds()).isEmpty();
        assertThat(result.unassignedVehicleIds()).isEmpty();
    }

    @Test
    @DisplayName("차량이 주문보다 적으면 차량 수만큼만 배정되고, 나머지 주문은 미배정으로 남는다")
    void fewerVehiclesThanOrders_assignsUpToVehicleCount() {
        // given: 주문 3건, 차량 2대, 모든 조합이 후보
        List<AssignmentCandidate> candidates = List.of(
                candidate(1L, 10L, 10.0), candidate(1L, 11L, 20.0),
                candidate(2L, 10L, 30.0), candidate(2L, 11L, 15.0),
                candidate(3L, 10L, 40.0), candidate(3L, 11L, 50.0)
        );

        // when
        OptimizationResult result = optimizer.solve(candidates, List.of(1L, 2L, 3L), List.of(10L, 11L));

        // then: 2건 배정, 주문 1건 미배정, 차량은 모두 사용
        assertThat(result.assignments()).hasSize(2);
        assertThat(result.unassignedOrderIds()).hasSize(1);
        assertThat(result.unassignedVehicleIds()).isEmpty();
        // 차량당 최대 1건 제약: 배정된 차량 ID에 중복이 없어야 한다
        assertThat(result.assignments()).extracting(Assignment::vehicleId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("모두 배정 가능하면 주문당 1대, 차량당 1건 제약을 지키며 전부 배정한다")
    void allAssignable_respectsOneToOneConstraints() {
        // given: 주문 2건, 차량 2대, 모든 조합이 후보
        List<AssignmentCandidate> candidates = List.of(
                candidate(1L, 10L, 10.0), candidate(1L, 11L, 12.0),
                candidate(2L, 10L, 11.0), candidate(2L, 11L, 13.0)
        );

        // when
        OptimizationResult result = optimizer.solve(candidates, List.of(1L, 2L), List.of(10L, 11L));

        // then
        assertThat(result.assignments()).hasSize(2);
        assertThat(result.assignments()).extracting(Assignment::orderId).doesNotHaveDuplicates();
        assertThat(result.assignments()).extracting(Assignment::vehicleId).doesNotHaveDuplicates();
        assertThat(result.unassignedOrderIds()).isEmpty();
        assertThat(result.unassignedVehicleIds()).isEmpty();
    }

    @Test
    @DisplayName("[정리본 10.5 예시] 그리디(35km)가 아니라 전체 최적(28km) 조합을 고른다")
    void choosesGlobalOptimum_notGreedy() {
        // given: 주문 A(1), B(2) / 차량 X(10), Y(20)
        //            X     Y
        //   A        5    20
        //   B        8    30
        // 그리디(A 먼저 가까운 차): A→X(5) + B→Y(30) = 35km
        // 전체 최적:                 A→Y(20) + B→X(8) = 28km
        List<AssignmentCandidate> candidates = List.of(
                candidate(1L, 10L, 5.0), candidate(1L, 20L, 20.0),
                candidate(2L, 10L, 8.0), candidate(2L, 20L, 30.0)
        );

        // when
        OptimizationResult result = optimizer.solve(candidates, List.of(1L, 2L), List.of(10L, 20L));

        // then: A→Y, B→X 조합이고 거리 합은 28km
        assertThat(result.assignments())
                .extracting(Assignment::orderId, Assignment::vehicleId)
                .containsExactlyInAnyOrder(tuple(1L, 20L), tuple(2L, 10L));
        assertThat(totalDistance(result)).isEqualTo(28.0);
    }

    @Test
    @DisplayName("[사전식 목표] 거리가 늘어나더라도 배정 건수를 먼저 최대화한다")
    void maximizesCountBeforeDistance() {
        // given: 주문 1은 X(1km) 또는 Y(100km), 주문 2는 X(1km)만 가능
        // - 1건만 배정하면 거리 1km로 짧지만,
        // - 2건을 모두 배정하려면 주문 1을 먼 Y(100km)로 보내야 한다 (합 101km)
        // 1순위 목표가 '배정 건수'이므로 101km라도 2건 배정이 정답이다
        List<AssignmentCandidate> candidates = List.of(
                candidate(1L, 10L, 1.0), candidate(1L, 20L, 100.0),
                candidate(2L, 10L, 1.0)
        );

        // when
        OptimizationResult result = optimizer.solve(candidates, List.of(1L, 2L), List.of(10L, 20L));

        // then
        assertThat(result.assignments())
                .extracting(Assignment::orderId, Assignment::vehicleId)
                .containsExactlyInAnyOrder(tuple(1L, 20L), tuple(2L, 10L));
        assertThat(totalDistance(result)).isEqualTo(101.0);
    }


    // ---------- 테스트 도우미 ----------

    /** 후보 조합을 짧게 만들기 위한 도우미 (주문 ID, 차량 ID, 거리 km) */
    private AssignmentCandidate candidate(Long orderId, Long vehicleId, double distanceKm) {
        return new AssignmentCandidate(orderId, vehicleId, distanceKm);
    }

    /** 배정 결과의 거리 합(km) */
    private double totalDistance(OptimizationResult result) {
        return result.assignments().stream().mapToDouble(Assignment::distanceKm).sum();
    }
}
