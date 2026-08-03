package com.tmsproject.dispatch.dto.response;


import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;

import java.time.LocalDateTime;

/**
 * 배차 목록 조회 응답의 각 항목. (API 명세서 3.3 참고)
 *
 * 주의: orderNo/vehicleNo는 연관 엔티티(TransportOrder/Vehicle)의 필드다.
 * DispatchRepository.findAllWithOrderAndVehicle()이 JOIN FETCH로 이미 함께 조회해왔기 때문에
 * 여기서 접근해도 LazyInitializationException이 발생하지 않는다.
 */
public record DispatchListItemResponse(
        Long id,
        String orderNo,
        String vehicleNo,
        DispatchStatus dispatchStatus,
        LocalDateTime assignedAt
) {
    public static DispatchListItemResponse from(Dispatch dispatch) {
        return new DispatchListItemResponse(
                dispatch.getId(),
                dispatch.getTransportOrder().getOrderNo(),
                dispatch.getVehicle().getVehicleNo(),
                dispatch.getDispatchStatus(),
                dispatch.getAssignedAt()
        );
    }
}
