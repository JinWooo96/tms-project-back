package com.tmsproject.vehicle.entity;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.tmsproject.support.TestFixtures.order;
import static com.tmsproject.support.TestFixtures.vehicle1Ton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vehicle 엔티티 단위 테스트: 용량 판단(canCarry)과 상태 전이 규칙.
 * canCarry는 수동 배정·자동 추천·확정 재검증이 모두 쓰는 판단 기준이라 경계값까지 검증한다.
 */
class VehicleTest {

    @Test
    @DisplayName("중량이 최대 적재중량과 정확히 같으면 실을 수 있다. (경계값)")
    void canCarry_exactMaxWeight_true() {
        Vehicle vehicle = vehicle1Ton("12가0001");
        TransportOrder order = order(new BigDecimal("1000.00"), 100, 100, 100);

        assertThat(vehicle.canCarry(order.totalWeight(), order.totalVolume())).isTrue();
    }

    @Test
    @DisplayName("중량이 최대 적재중량을 0.01kg이라도 넘으면 실을 수 없다")
    void canCarry_overWeight_false() {
        Vehicle vehicle = vehicle1Ton("12가0001");
        TransportOrder order = order(new BigDecimal("1000.01"), 100, 100, 100);

        assertThat(vehicle.canCarry(order.totalWeight(), order.totalVolume())).isFalse();
    }

    @Test
    @DisplayName("중량은 여유 있어도 부피가 적재함을 넘으면 실을 수 없다")
    void canCarry_overVolume_false() {
        Vehicle vehicle = vehicle1Ton("12가0001");
        // 160 x 280 x 171 = 7,660,800cm³ > 적재함 7,616,000cm³
        TransportOrder order = order(new BigDecimal("10.00"), 160, 280, 171);

        assertThat(vehicle.canCarry(order.totalWeight(), order.totalVolume())).isFalse();
    }

    @Test
    @DisplayName("AVAILABLE 차량만 배차 상태(ASSIGNED)로 바뀔 수 있다")
    void markAsAssigned_twice_throws() {
        Vehicle vehicle = vehicle1Ton("12가0001");
        vehicle.markAsAssigned(); // 배차 확정 ASSIGNED

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ASSIGNED);
        // 이미 배차 중인 차량을 다시 배정하려 하면 예외가 발생해야 한다.
        assertThatThrownBy(vehicle::markAsAssigned)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_AVAILABLE);
    }
}
