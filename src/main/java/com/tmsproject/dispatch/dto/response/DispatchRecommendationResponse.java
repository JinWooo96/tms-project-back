package com.tmsproject.dispatch.dto.response;

import java.util.List;

/**
 * 자동 배차 추천 응답. (API 명세서 v2, 4.1절 참고)
 * 실제 Dispatch 레코드를 생성하지 않는, 순수 "계산 결과"임에 유의한다.
 */
public record DispatchRecommendationResponse(
        List<Item> recommendations,
        List<Long> unassignedOrderIds,
        List<Long> unassignedVehicleIds
) {
    public record Item(
            Long orderId,
            String orderNo,
            Long vehicleId,
            String vehicleNo,
            double distanceKm
    ) {

    }
}
