package com.tmsproject.vehicle.repository;

import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // 특정 상태의 차량 목록 조회. 가능 차량 필터링(중량/부피)의 1차 후보군을 가져올 때 사용.
    // 실제 중량/부피 조건은 Vehicle.canCarry()로 애플리케이션 레벨에서 필터링한다. (아래 설명 참고)
    List<Vehicle> findByStatus(VehicleStatus status);

    // 차량 등록 시 중복 차량번호 검증에 사용
    boolean existsByVehicleNo(String vehicleNo);
}
