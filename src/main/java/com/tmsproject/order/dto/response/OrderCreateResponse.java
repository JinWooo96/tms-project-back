package com.tmsproject.order.dto.response;

import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;

import java.time.LocalDateTime;

/**
 * 운송주문 등록 응답. (API 명세서 1.1 참고 - 등록 직후엔 최소 정보만 반환)
 */
public record OrderCreateResponse(
        Long id,
        String orderNo,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderCreateResponse from(TransportOrder order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
