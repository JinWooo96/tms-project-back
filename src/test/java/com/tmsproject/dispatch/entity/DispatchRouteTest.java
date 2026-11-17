package com.tmsproject.dispatch.entity;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.tmsproject.support.TestFixtures.order;
import static com.tmsproject.support.TestFixtures.vehicle1Ton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DispatchRoute(멀티드롭 경로) 엔티티 단위 테스트.
 * 핵심 원칙: 차량 상태(AVAILABLE ↔ ASSIGNED)는 개별 배차가 아니라 경로가 "딱 한 번만" 바꾼다.
 */
class DispatchRouteTest {

    @Test
    @DisplayName("경로 확정 시 소속 배차 전부가 방문순서대로 확정되고, 차량은 한 번만 ASSIGNED로 바뀐다")
    void confirmWith_confirmsAllDispatchesInOrder() {
        // given: 한 차량에 주문 3건
        Vehicle vehicle = vehicle1Ton("12가0001");
        List<TransportOrder> orders = List.of(order(), order(), order());
        List<Dispatch> dispatches = orders.stream()
                .map(o -> Dispatch.builder().transportOrder(o).vehicle(vehicle).build())
                .toList();
        DispatchRoute route = DispatchRoute.builder().vehicle(vehicle).build();
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 9, 0);

        // when: 차량 상태를 배차마다 바꿨다면 두 번째 배차에서 VEHICLE_NOT_AVAILABLE 예외가 났을 것
        route.confirmWith(dispatches, List.of(start, start.plusMinutes(30), start.plusMinutes(60)));

        // then
        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.CONFIRMED);
        assertThat(route.getDispatchList()).hasSize(3);
        assertThat(dispatches).extracting(Dispatch::getSequence).containsExactly(1, 2, 3);
        assertThat(dispatches).extracting(Dispatch::getDispatchStatus).containsOnly(DispatchStatus.CONFIRMED);
        assertThat(orders).extracting(TransportOrder::getStatus).containsOnly(OrderStatus.DISPATCHED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ASSIGNED);
    }

    @Test
    @DisplayName("경로 취소 시 소속 배차·주문이 모두 원상복구되고, 차량은 AVAILABLE이 된다")
    void cancel_revertsAll() {
        Vehicle vehicle = vehicle1Ton("12가0001");
        List<TransportOrder> orders = List.of(order(), order());
        DispatchRoute route = confirmedRoute(vehicle, orders);

        route.cancel();

        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.CANCELLED);
        assertThat(route.getDispatchList()).extracting(Dispatch::getDispatchStatus)
                .containsOnly(DispatchStatus.CANCELLED);
        assertThat(orders).extracting(TransportOrder::getStatus).containsOnly(OrderStatus.RECEIVED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("[버그①] 이미 취소된 경로는 다시 취소할 수 없다")
    void cancel_twice_throws() {
        DispatchRoute route = confirmedRoute(vehicle1Ton("12가0001"), List.of(order()));
        route.cancel();

        assertThatThrownBy(route::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUTE_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("[버그① 실제 피해] 취소된 경로를 또 취소해도, 같은 차량이 배정된 다른 경로의 차량 상태는 바뀌지 않는다")
    void cancelTwice_doesNotReleaseVehicleOfAnotherRoute() {
        // given: 경로 A 확정 → 취소 → 같은 차량으로 경로 B 확정
        Vehicle vehicle = vehicle1Ton("12가0001");
        DispatchRoute routeA = confirmedRoute(vehicle, List.of(order()));
        routeA.cancel();
        confirmedRoute(vehicle, List.of(order())); // 경로 B: 차량은 다시 ASSIGNED

        // when: 누군가 이미 취소된 경로 A를 또 취소하려 한다 (화면 중복 클릭, 재시도 요청 등)
        // then: 막혀야 하고, 경로 B를 운행 중인 차량은 계속 ASSIGNED여야 한다
        assertThatThrownBy(routeA::cancel).isInstanceOf(BusinessException.class);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ASSIGNED);
    }

    // ---------- 테스트 도우미 ----------

    /** 주어진 차량과 주문들로 확정된 경로를 만든다 (방문 예정 시각은 30분 간격) */
    private DispatchRoute confirmedRoute(Vehicle vehicle, List<TransportOrder> orders) {
        List<Dispatch> dispatches = orders.stream()
                .map(o -> Dispatch.builder().transportOrder(o).vehicle(vehicle).build())
                .toList();
        DispatchRoute route = DispatchRoute.builder().vehicle(vehicle).build();
        LocalDateTime start = LocalDateTime.now();
        List<LocalDateTime> arrivals = java.util.stream.IntStream.range(0, dispatches.size())
                .mapToObj(i -> start.plusMinutes(30L * i))
                .toList();
        route.confirmWith(dispatches, arrivals);
        return route;
    }
}
