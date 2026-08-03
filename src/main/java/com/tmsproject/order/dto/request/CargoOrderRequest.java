package com.tmsproject.order.dto.request;

import com.tmsproject.order.entity.TransportOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record CargoOrderRequest(
        @NotBlank(message = "상차지 주소는 필수입니다.")
        String originAddress,
        Double originLat,
        Double originLng,

        @NotBlank(message = "도착지 주소는 필수입니다.")
        String destAddress,
        Double destLat,
        Double destLng,

        LocalDateTime requestedPickupAt,
        LocalDateTime requestedDeliveryAt,

        @NotEmpty(message = "화물은 최소 1개 이상 등록해야 합니다.")
        @Valid // 리스트 안의 CargoRequest 각각에 대해서도 검증(@NotNull, @Positive 등)을 수행하라는 의미
        List<CargoRequest> cargoList
) {
    /**
     * 이 요청을 TransportOrder(+ Cargo 리스트) 엔티티로 변환한다.
     * orderNo는 여기서 세팅하지 않는다 (TransportOrderService.registerOrder()에서 서버가 생성)
     */
    public TransportOrder toEntity() {
        TransportOrder order = TransportOrder.builder()
                .originAddress(originAddress)
                .originLat(originLat)
                .originLng(originLng)
                .destAddress(destAddress)
                .destLat(destLat)
                .destLng(destLng)
                .requestedPickupAt(requestedPickupAt)
                .requestedDeliveryAt(requestedDeliveryAt)
                .build();

        cargoList.forEach(cargoRequest -> order.addCargo(cargoRequest.toEntity()));

        return order;
    }
}
