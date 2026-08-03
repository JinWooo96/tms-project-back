package com.tmsproject.vehicle.entity;


import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 차량 엔티티. 배차 가능 여부 판단(중량/부피 조건)의 기준이 되는 정보를 가진다.
 */
@Entity
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_no", nullable = false, unique = true, length = 20)
    private String vehicleNo;

    // Enum 타입으로 하지 않은 이유
    // 생명주기가 있는 값이 아니라, 카테고리성 값이기 때문에
    // 변경이 일어날 수 있다. (변경이 없다고 한다면 Enum으로 가도 무방)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String vehicleType;

    @Column(name = "max_weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "bed_width_cm", nullable = false)
    private Integer bedWidthCm;

    @Column(name = "bed_length_cm", nullable = false)
    private Integer bedLengthCm;

    @Column(name = "bed_height_cm", nullable = false)
    private Integer bedHeightCm;

    @Column(name = "base_address")
    private String baseAddress;

    @Column(name = "base_lat")
    private Double baseLat;

    @Column(name = "base_lng")
    private Double baseLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VehicleStatus status;

    @Builder(access = AccessLevel.PUBLIC)
    private Vehicle(String vehicleNo, String vehicleType, BigDecimal maxWeightKg,
                    Integer bedWidthCm, Integer bedLengthCm, Integer bedHeightCm,
                    String baseAddress, Double baseLat, Double baseLng) {
        this.vehicleNo = vehicleNo;
        this.vehicleType = vehicleType;
        this.maxWeightKg = maxWeightKg;
        this.bedWidthCm = bedWidthCm;
        this.bedLengthCm = bedLengthCm;
        this.bedHeightCm = bedHeightCm;
        this.baseAddress = baseAddress;
        this.baseLat = baseLat;
        this.baseLng = baseLng;
        this.status = VehicleStatus.AVAILABLE; // 차량 등록 시 기본은 운행가능 상태
    }

    /** 적재함 총부피 (cm^3) */
    public long bedVolume() {
        return (long) bedWidthCm * bedLengthCm * bedHeightCm;
    }

    /** 이 차량이 주어진 총중량/총부피를 감당할 수 있는지 판단 */
    public boolean canCarry(BigDecimal totalWeightKg, long totalVolume) {
        boolean weightOk = this.maxWeightKg.compareTo(totalWeightKg) >= 0;
        boolean volumeOk = this.bedVolume() >= totalVolume;
        return weightOk && volumeOk;
    }

    /** 배차 확정 시 호출 */
    public void markAsAssigned() {
        if (this.status != VehicleStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_AVAILABLE);
        }
        this.status = VehicleStatus.ASSIGNED;
    }

    /** 배차 취소 시 호출 */
    public void markAsAvailable() {
        this.status = VehicleStatus.AVAILABLE;
    }
}