package com.tmsproject.order.dto.response;

import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        LocalDateTime createdAt,
        BigDecimal totalWeightKg, // 추가: 주문 전체 중량(kg)
        BigDecimal totalVolumeM3  // 추가: 주문 전체 부피(㎥)
) {
    // 주의: cargoList를 읽으므로 반드시 트랜잭션 안(서비스)에서 호출해야 한다.
    public static OrderSummaryResponse from(TransportOrder order) {
        // 엔티티의 totalVolume()은 ㎤(cm³) 단위이므로 1,000,000으로 나눠 ㎥로 변환한다.
        BigDecimal volumeM3 = BigDecimal.valueOf(order.totalVolume())
                .divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);

        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getOriginAddress(),
                order.getDestAddress(),
                order.getStatus(),
                order.getRequestedPickupAt(),
                order.getCreatedAt(),
                order.totalWeight(), // 화물별 (개당 중량 x 개수) 합계
                volumeM3
        );
    }
}
