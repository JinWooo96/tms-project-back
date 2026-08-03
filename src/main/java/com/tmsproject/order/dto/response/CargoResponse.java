package com.tmsproject.order.dto.response;

import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.cargo.entity.CargoType;

import java.math.BigDecimal;

public record CargoResponse(
        Long id,
        Integer widthCm,
        Integer lengthCm,
        Integer heightCm,
        BigDecimal weightKg,
        Integer quantity,
        CargoType cargoType
) {
    public static CargoResponse from(Cargo cargo) {
        return new CargoResponse(
                cargo.getId(),
                cargo.getWidthCm(),
                cargo.getLengthCm(),
                cargo.getHeightCm(),
                cargo.getWeightKg(),
                cargo.getQuantity(),
                cargo.getCargoType()
        );
    }
}
