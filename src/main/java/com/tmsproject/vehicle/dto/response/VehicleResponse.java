package com.tmsproject.vehicle.dto.response;


import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;

import java.math.BigDecimal;

public record VehicleResponse(
        Long id,
        String vehicleNo,
        String vehicleType,
        BigDecimal maxWeightKg,
        Integer bedWidthCm,
        Integer bedLengthCm,
        Integer bedHeightCm,
        String baseAddress,
        Double baseLat,
        Double baseLng,
        VehicleStatus status
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getVehicleNo(),
                vehicle.getVehicleType(),
                vehicle.getMaxWeightKg(),
                vehicle.getBedWidthCm(),
                vehicle.getBedLengthCm(),
                vehicle.getBedHeightCm(),
                vehicle.getBaseAddress(),
                vehicle.getBaseLat(),
                vehicle.getBaseLng(),
                vehicle.getStatus()
        );
    }
}
