package com.tmsproject.order.dto.response;

import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;

import java.time.LocalDateTime;

/**
 * 운송주문 목록 조회 응답의 각 항목. (API 명세서 1.2 참고)
 * 목록에서는 화물 상세, 좌표 등은 굳이 내려주지 않아 응답 크기를 줄인다.
 */
public record OrderSummaryResponse(
        Long id,
        String orderNo,
        String originAddress,
        String destAddress,
        OrderStatus status,
        LocalDateTime requestedPickupAt,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponse from(TransportOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getOriginAddress(),
                order.getDestAddress(),
                order.getStatus(),
                order.getRequestedPickupAt(),
                order.getCreatedAt()
        );
    }
}
