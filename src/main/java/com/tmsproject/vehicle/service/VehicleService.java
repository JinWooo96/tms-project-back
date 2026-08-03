package com.tmsproject.vehicle.service;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.order.service.TransportOrderService;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import com.tmsproject.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TransportOrderService transportOrderService;

    /**
     * 특정 운송주문의 화물을 기준으로, 배차 가능한(중량/부피 조건 충족 + AVAILABLE 상태) 차량 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getAvailableVehiclesFor(Long orderId) {
        TransportOrder order = transportOrderService.getOrder(orderId);

        BigDecimal totalWeight = order.totalWeight();
        long totalVolume = order.totalVolume();

        List<Vehicle> resultList = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .filter(vehicle -> vehicle.canCarry(totalWeight, totalVolume))
                .toList();

        return resultList;
    }

    /**
     * 차량을 등록한다. (요구사항 정의서 1차 범위에는 없었으나, 배차 테스트를 위한 차량 마스터 데이터 등록 수단으로 추가)
     * 차량번호 중복은 DB의 UNIQUE 제약이 최종 방어선이지만, 사용자에게 더 명확한 에러를 주기 위해
     * 저장 전에 한 번 더 애플리케이션 레벨에서 검증한다.
     */
    @Transactional
    public Vehicle registerVehicle(Vehicle vehicle) {
        // 차량번호 중복 체크
        if (vehicleRepository.existsByVehicleNo(vehicle.getVehicleNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VEHICLE_NO);
        }
        return vehicleRepository.save(vehicle);
    }

    /** 차량 전체 목록 조회 (관리 화면용) */
    @Transactional(readOnly = true)
    public List<Vehicle> getVehicles(VehicleStatus status) {
        if (status == null) {
            return vehicleRepository.findAll();
        }
        return vehicleRepository.findByStatus(status);
    }

    /* 차량 단건 조회. 없으면 예외 발생 */
    @Transactional(readOnly = true)
    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));
    }
}
