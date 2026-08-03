package com.tmsproject.order.dto.request;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.order.entity.TransportOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운송주문 등록 요청. (API 명세서 1.1 참고)
 *
 * v2: originLat/originLng/destLat/destLng 필드를 제거했다.
 * 클라이언트는 주소만 보내고, 좌표는 Controller가 GeocodingService로 변환하여
 * toEntity() 호출 시 GeoPoint로 전달한다.
 */
public record CreateOrderRequest(
        @NotBlank(message = "상차지 주소는 필수입니다.")
        String originAddress,

        @NotBlank(message = "도착지 주소는 필수입니다.")
        String destAddress,

        LocalDateTime requestedPickupAt,
        LocalDateTime requestedDeliveryAt,

        @NotEmpty(message = "화물은 최소 1개 이상 등록해야 합니다.")
        @Valid // 리스트 안의 CargoRequest 각각에 대해서도 검증(@NotNull, @Positive 등)을 수행하라는 의미
        List<CargoRequest> cargoList
) {
    /**
     * 이 요청을 TransportOrder(+ Cargo 리스트) 엔티티로 변환한다.
     * orderNo는 여기서 세팅하지 않는다 (TransportOrderService.registerOrder()에서 서버가 생성)
     *
     * @param originPoint GeocodingService.geocode(originAddress)의 결과
     * @param destPoint   GeocodingService.geocode(destAddress)의 결과
     */
    public TransportOrder toEntity(GeoPoint originPoint, GeoPoint destPoint) {
        TransportOrder order = TransportOrder.builder()
                .originAddress(originAddress)
                .originLat(originPoint.lat())
                .originLng(originPoint.lng())
                .destAddress(destAddress)
                .destLat(destPoint.lat())
                .destLng(destPoint.lng())
                .requestedPickupAt(requestedPickupAt)
                .requestedDeliveryAt(requestedDeliveryAt)
                .build();

        cargoList.forEach(cargoRequest -> order.addCargo(cargoRequest.toEntity()));

        return order;
    }
}
