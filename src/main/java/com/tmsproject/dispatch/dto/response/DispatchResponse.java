package com.tmsproject.dispatch.dto.response;


import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;

import java.time.LocalDateTime;

/**
 * 배차 확정 응답. (API 명세서 3.1 참고)
 *
 * 주의: getTransportOrder()/getVehicle()은 LAZY 연관관계이지만,
 * DispatchService.confirmDispatch()에서 이미 조회해둔 실제 엔티티 객체를
 * Dispatch.builder()에 그대로 넘겨서 만든 것이므로 프록시가 아니다.
 * 따라서 트랜잭션 밖(Controller)에서 getId()를 호출해도 안전하다.
 */
public record DispatchResponse(
        Long id,
        Long orderId,
        Long vehicleId,
        DispatchStatus dispatchStatus,
        LocalDateTime assignedAt
) {
    public static DispatchResponse from(Dispatch dispatch) {
        return new DispatchResponse(
                dispatch.getId(),
                dispatch.getTransportOrder().getId(),
                dispatch.getVehicle().getId(),
                dispatch.getDispatchStatus(),
                dispatch.getAssignedAt()
        );
    }
}
