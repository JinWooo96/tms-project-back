package com.tmsproject.vehicle.dto.request;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.vehicle.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 차량 등록 요청.
 * 주의: 이 화면/API는 원래 요구사항 정의서의 1차 범위(운송주문/배차)에는 없던 기능이다.
 * 배차 테스트를 위해 차량 마스터 데이터를 등록할 수단이 필요해져서 추가했다.
 * (요구사항 정의서 4장 "추후 확장 요구사항"에 별도 반영 필요)
 *
 * v2: baseAddress(차고지 주소) 추가. 좌표(baseLat/baseLng)는 클라이언트가 보내지 않고,
 * Controller가 GeocodingService로 변환한 GeoPoint를 toEntity()에 전달받아 채운다.
 */
public record CreateVehicleRequest(
        @NotBlank(message = "차량번호는 필수입니다.")
        String vehicleNo,

        @NotBlank(message = "차량유형은 필수입니다.")
        String vehicleType,

        @NotNull(message = "최대 적재중량은 필수입니다.") @Positive
        BigDecimal maxWeightKg,

        @NotNull(message = "적재함 가로 길이는 필수입니다.") @Positive
        Integer bedWidthCm,

        @NotNull(message = "적재함 세로 길이는 필수입니다.") @Positive
        Integer bedLengthCm,

        @NotNull(message = "적재함 높이는 필수입니다.") @Positive
        Integer bedHeightCm,

        @NotBlank(message = "차고지 주소는 필수입니다.")
        String baseAddress
) {
    /** basePoint: GeocodingService.geocode(baseAddress)의 결과를 Controller에서 전달받는다. */
    public Vehicle toEntity(GeoPoint basePoint) {
        return Vehicle.builder()
                .vehicleNo(vehicleNo)
                .vehicleType(vehicleType)
                .maxWeightKg(maxWeightKg)
                .bedWidthCm(bedWidthCm)
                .bedLengthCm(bedLengthCm)
                .bedHeightCm(bedHeightCm)
                .baseAddress(baseAddress)
                .baseLat(basePoint.lat())
                .baseLng(basePoint.lng())
                .build();
    }
}
