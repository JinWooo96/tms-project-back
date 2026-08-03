package com.tmsproject.dispatch.optimization.dto;

import java.util.List;

/**
 * DispatchOptimizer.solve()의 계산 결과
 * 실제 Dispatch 레코드를 생성하지 않는, 순수 "계산 결과"임에 유의한다.
 */
public record OptimizationResult(
        List<Assignment> assignments,
        List<Long> unassignedOrderIds,
        List<Long> unassignedVehicleIds
) {
    /** 최종적으로 선택된 (주문, 차량) 배정 하나. */
    public record Assignment(Long orderId, Long vehicleId, double distanceKm) {
    }
}
