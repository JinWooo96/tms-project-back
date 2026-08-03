package com.tmsproject.dispatch.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 배차 확정 요청. (API 명세서 3.1 참고)
 */
public record ConfirmDispatchRequest(
        @NotNull(message = "운송주문 ID는 필수입니다.")
        Long orderId,

        @NotNull(message = "차량 ID는 필수입니다.")
        Long vehicleId
) {
}
