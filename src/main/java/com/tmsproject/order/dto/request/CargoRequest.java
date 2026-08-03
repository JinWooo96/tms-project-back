package com.tmsproject.order.dto.request;

import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.cargo.entity.CargoType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 운송주문 등록 시, 화물 1개에 대한 입력값.
 * record를 사용해 불변(immutable) DTO로 선언 (getter/생성자가 자동으로 생김)
 */
public record CargoRequest (
        @NotNull(message = "가로 길이는 필수입니다.") @Positive
        Integer widthCm,

        @NotNull(message = "세로 길이는 필수입니다.") @Positive
        Integer lengthCm,

        @NotNull(message = "높이는 필수입니다.") @Positive
        Integer heightCm,

        @NotNull(message = "중량은 필수입니다.") @Positive
        BigDecimal weightKg,

        @NotNull(message = "개수는 필수입니다.") @Positive
        Integer quantity,

        @NotNull(message = "화물 유형은 필수입니다.")
        CargoType cargoType
) {
    /** 이 요청 DTO를 Cargo 엔티티로 변환한다. */
    public Cargo toEntity() {
        return Cargo.builder()
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .weightKg(weightKg)
                .quantity(quantity)
                .cargoType(cargoType)
                .build();
    }
}
