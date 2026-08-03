package com.tmsproject.order.dto.response;

import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운송주문 상세 조회 응답. (API 명세서 1.3 참고)
 */
public record OrderResponse(
        Long id,
        String orderNo,
        String originAddress,
        Double originLat,
        Double originLng,
        String destAddress,
        Double destLat,
        Double destLng,
        LocalDateTime requestedPickupAt,
        LocalDateTime requestedDeliveryAt,
        OrderStatus status,
        List<CargoResponse> cargoList,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(TransportOrder order) {
        List<CargoResponse> cargoResponses = order.getCargoList().stream()
                .map(CargoResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getOriginAddress(),
                order.getOriginLat(),
                order.getOriginLng(),
                order.getDestAddress(),
                order.getDestLat(),
                order.getDestLng(),
                order.getRequestedPickupAt(),
                order.getRequestedDeliveryAt(),
                order.getStatus(),
                cargoResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
