package com.tmsproject.dispatch.entity;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.support.TestFixtures;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.tmsproject.support.TestFixtures.order;
import static com.tmsproject.support.TestFixtures.vehicle1Ton;
import static org.assertj.core.api.Assertions.*;

/**
 * Dispatch(단건 배차) 엔티티 단위 테스트.
 * confirm()/cancel()이 배차·주문·차량 세 엔티티의 상태를 함께 바꾸는지(원자성) 검증한다.
 */
class DispatchTest {

    @Test
    @DisplayName("단건 확정 시 배차 CONFIRMED, 주문 DISPATCHED, 차량 ASSIGNED로 함께 바뀐다")
    void confirm_changesAllThreeStates() {
        TransportOrder order = order(); // 기본 운송 주문 생성
        Vehicle vehicle = vehicle1Ton("12가0001");
        Dispatch dispatch = Dispatch.builder().transportOrder(order).vehicle(vehicle).build(); // 배차 생성

        dispatch.confirm(); // 배차 확정

        assertThat(dispatch.getDispatchStatus()).isEqualTo(DispatchStatus.CONFIRMED);
        assertThat(dispatch.getAssignedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ASSIGNED);
    }

    @Test
    @DisplayName("이미 배차된 주문은 다시 확정할 수 없다")
    void confirm_alreadyDispatchedOrder_throws() {
        TransportOrder order = order();
        order.markAsDispatched(); // 다른 배차로 이미 확정된 상황
        Dispatch dispatch = Dispatch.builder().transportOrder(order).vehicle(vehicle1Ton("12가0001")).build();

        assertThatThrownBy(dispatch::confirm)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_DISPATCHED);
    }

    @Test
    @DisplayName("단건 취소 시 주문은 RECEIVED, 차량은 AVAILABLE로 원상복구되고, 두 번 취소할 수 없다")
    void cancel_revertsStates_andCannotCancelTwice() {
        TransportOrder order = order();
        Vehicle vehicle = vehicle1Ton("12가0001");
        Dispatch dispatch = Dispatch.builder().transportOrder(order).vehicle(vehicle).build();
        dispatch.confirm();

        dispatch.cancel(); // 배차 취소

        assertThat(dispatch.getDispatchStatus()).isEqualTo(DispatchStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThatThrownBy(dispatch::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPATCH_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("[버그②] 경로에 속한 배차는 단건 취소할 수 없고, 차량 상태도 그대로 유지된다")
    void cancel_dispatchInRoute_throws() {
        // given: 주문 2건이 한 경로로 확정된 상태
        Vehicle vehicle = vehicle1Ton("12가0001");
        TransportOrder order1 = order();
        TransportOrder order2 = order();
        Dispatch dispatch1 = Dispatch.builder().transportOrder(order1).vehicle(vehicle).build();
        Dispatch dispatch2 = Dispatch.builder().transportOrder(order2).vehicle(vehicle).build();
        DispatchRoute route = DispatchRoute.builder().vehicle(vehicle).build();
        LocalDateTime now = LocalDateTime.now();
        route.confirmWith(List.of(dispatch1, dispatch2), List.of(now, now.plusHours(1)));

        // when & then: 그중 하나만 단건 취소하려 하면 막혀야 한다
        assertThatThrownBy(dispatch1::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPATCH_IN_ROUTE);
        // 나머지 주문이 남아 있으므로 차량은 여전히 배차 중이어야 한다
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ASSIGNED);
    }
}
