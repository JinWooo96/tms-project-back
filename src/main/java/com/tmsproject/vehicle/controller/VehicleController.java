package com.tmsproject.vehicle.controller;


import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.geocoding.service.GeocodingService;
import com.tmsproject.geocoding.service.KakaoGeocodingService;
import com.tmsproject.global.response.ApiResponse;
import com.tmsproject.vehicle.dto.request.CreateVehicleRequest;
import com.tmsproject.vehicle.dto.response.VehicleResponse;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import com.tmsproject.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final GeocodingService geocodingService;

    /**
     * POST /api/vehicles - 차량 등록
     * (요구사항 정의서 1차 범위에는 없었으나, 배차 테스트를 위한 차량 마스터 데이터 등록 수단으로 추가)
     *
     * v2: 차고지 주소를 지오코딩하여 좌표를 함께 저장한다.
     * 지오코딩(외부 API 호출)은 트랜잭션이 없는 이 Controller 계층에서 먼저 끝내고,
     * 좌표까지 다 채운 완성된 엔티티를 VehicleService에 넘겨서 그때부터 짧은 트랜잭션을 연다.
     * (백엔드 아키텍처 문서 v2, 3.2절 "지오코딩 호출을 트랜잭션 밖에서 수행" 참고)
     */
    @PostMapping("/api/vehicles")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request) {

        GeoPoint basePoint = geocodingService.geocode(request.baseAddress());
        Vehicle vehicle = request.toEntity(basePoint);
        Vehicle savedVehicle = vehicleService.registerVehicle(vehicle);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(VehicleResponse.from(savedVehicle)));
    }

    /** GET /api/vehicles/available?orderId= - 배차 가능 차량 조회 (API 명세서 2.1) */
    @GetMapping("/api/vehicles/available")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAvailableVehicles(
            @RequestParam Long orderId) {
        log.info("getAvailableVehicles...orderId={}", orderId);

        List<Vehicle> vehicles = vehicleService.getAvailableVehiclesFor(orderId);
        List<VehicleResponse> response = vehicles.stream().map(VehicleResponse::from).toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/vehicles?status= - 차량 전체 조회 (API 명세서 2.2) */
    @GetMapping("/api/vehicles")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getVehicles(
            @RequestParam(required = false) VehicleStatus status) {

        List<Vehicle> vehicles = vehicleService.getVehicles(status);
        List<VehicleResponse> response = vehicles.stream().map(VehicleResponse::from).toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** DELETE /api/vehicles/{vehicleId} - 차량 삭제 (API 명세서 2.3 추가 필요). 성공 시 본문 없이 204 */
    @DeleteMapping("/api/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(vehicleId);

        return ResponseEntity.noContent().build();
    }
}