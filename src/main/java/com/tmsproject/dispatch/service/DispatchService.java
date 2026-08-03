package com.tmsproject.dispatch.service;

import com.tmsproject.dispatch.dto.response.DispatchRecommendationResponse;
import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;
import com.tmsproject.dispatch.optimization.DispatchOptimizer;
import com.tmsproject.dispatch.optimization.dto.AssignmentCandidate;
import com.tmsproject.dispatch.optimization.dto.OptimizationResult;
import com.tmsproject.dispatch.repository.DispatchRepository;
import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.global.util.DistanceCalculator;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.order.service.TransportOrderService;
import com.tmsproject.vehicle.entity.Vehicle;
import com.tmsproject.vehicle.entity.VehicleStatus;
import com.tmsproject.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final TransportOrderService transportOrderService;
    private final VehicleService vehicleService;
    private final DispatchRepository dispatchRepository;
    private final DispatchOptimizer dispatchOptimizer;

    /**
     * 배차를 확정한다.
     * 프론트에서 이미 필터링된 차량만 보여주더라도, 그 사이 다른 요청으로 상태가 바뀌었을 수 있으므로
     * 여기서 중량/부피/상태 조건을 다시 한번 검증한다. (API 명세서 3.1 참고)
     */
    @Transactional
    public Dispatch confirmDispatch(Long orderId, Long vehicleId) {
        TransportOrder order = transportOrderService.getOrder(orderId);
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);

        // 서버 재검증: 중량/부피 조건 (프론트 필터링을 신뢰하지 않는다)
        if (!vehicle.canCarry(order.totalWeight(), order.totalVolume())) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }

        // 주문/차량 상태 검증은 Dispatch.confirm() 내부의 order.markAsDispatched(), vehicle.markAsAssigned()에서
        // 각각 처리한다 (RECEIVED가 아니거나 AVAILABLE이 아니면 여기서 예외 발생)

        Dispatch dispatch = Dispatch.builder()
                .transportOrder(order)
                .vehicle(vehicle)
                .build();

        dispatch.confirm(); // Dispatch/TransportOrder/Vehicle 세 엔티티 상태를 원자적으로 변경

        return dispatchRepository.save(dispatch);
    }

    /**
     * 배차를 취소한다.
     * findByIdWithOrderAndVehicle()로 transportOrder, vehicle까지 함께 조회한다.
     * (dispatch.cancel() 내부에서 이 두 연관 엔티티의 메서드를 호출하기 때문에,
     *  프록시가 아닌 완전히 로딩된 엔티티 상태로 시작해야 안전하다.)
     */
    @Transactional
    public Dispatch cancelDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepository.findByIdWithOrderAndVehicle(dispatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_NOT_FOUND));

        dispatch.cancel(); // 내부에서 이미 취소된 배차인지 검증 후, order/vehicle 상태 원상복구

        // 참고: dispatch는 이미 영속성 컨텍스트가 관리하는 객체(managed entity)이므로
        // 트랜잭션 커밋 시점에 변경분이 자동 반영된다 (더티 체킹). save() 호출은 필수는 아니지만
        // "여기서 상태가 확정된다"는 의도를 코드로 명확히 드러내기 위해 명시적으로 호출한다.
        return dispatchRepository.save(dispatch);
    }

    /** 배차 목록 조회 (상태/차량 필터, API 명세서 3.3) */
    @Transactional(readOnly = true)
    public List<Dispatch> getDispatches(DispatchStatus status, Long vehicleId) {
        return dispatchRepository.findAllWithOrderAndVehicle(status, vehicleId);
    }

    /**
     * v2: 자동 배차 추천.
     * selectedOrderIds가 null이거나 비어있으면 "미배차 주문 전체"를 대상으로 계산한다 (전체 추천).
     * selectedOrderIds가 있으면 그 주문들만 대상으로 계산한다 (선택 추천).
     * canCarry()로 1차 필터링한 후보만 DispatchOptimizer(OR-Tools CP-SAT)에 넘겨
     * 총 이동거리가 최소가 되는 조합을 계산한다. 실제 Dispatch 레코드는 생성하지 않는다.
     * (API 명세서 v2, 4.1절 참고)
     */
    @Transactional(readOnly = true)
    public DispatchRecommendationResponse getRecommendations(List<Long> selectedOrderIds) {
        boolean isSelectiveMode = selectedOrderIds != null && !selectedOrderIds.isEmpty();

        // 배차 가능한 주문 찾기
        List<TransportOrder> targetOrders = isSelectiveMode
                ? transportOrderService.getReceivedOrdersByIds(selectedOrderIds)
                : transportOrderService.getReceivedOrders();

        if (targetOrders.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_PENDING_ORDERS);
        }

        // 배차 가능한 차량 찾기
        List<Vehicle> availableVehicles = vehicleService.getVehicles(VehicleStatus.AVAILABLE);

        if (availableVehicles.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_VEHICLES);
        }

        // 나중에 (id -> 엔티티)로 다시 찾아보기 위한 맵. 추천 결과에 orderNo/vehicleNo를 채울 때 재사용한다.
        Map<Long, TransportOrder> orderById = targetOrders.stream()
                .collect(Collectors.toMap(TransportOrder::getId, order -> order));
        Map<Long, Vehicle> vehicleById = availableVehicles.stream()
                .collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle));

        List<AssignmentCandidate> candidates = new ArrayList<>();
        // 주문을 순회하면서 후보를 만든다.
        for (TransportOrder order : targetOrders) {
            if (order.getOriginLat() == null || order.getOriginLng() == null) {
                continue; // 좌표가 없는 주문은 거리 계산이 불가능하므로 후보에서 제외
            }
            for (Vehicle vehicle : availableVehicles) {
                if (vehicle.getBaseLat() == null || vehicle.getBaseLng() == null) {
                    continue;
                }
                if (!vehicle.canCarry(order.totalWeight(), order.totalVolume())) {
                    continue; // 용량 초과 조합은 후보에서 제외 (v1로직 재사용)
                }
                // 직선 거리 계산
                double distanceKm = DistanceCalculator.haversine(
                        vehicle.getBaseLat(), vehicle.getBaseLng(),
                        order.getOriginLat(), order.getOriginLng()
                );
                candidates.add(new AssignmentCandidate(order.getId(), vehicle.getId(), distanceKm));
            }
        }

        // 선택 모드에서는 "사용자가 원래 선택했던 ID 목록" 그대로를 orderIds로 써서,
        // 이미 배차되었거나 존재하지 않아 targetOrders에서 빠진 주문도 unassignedOrderIds에 정직하게 남게 한다.
        List<Long> orderIds = isSelectiveMode
                ? selectedOrderIds
                : targetOrders.stream().map(TransportOrder::getId).toList();
        List<Long> vehicleIds = availableVehicles.stream().map(Vehicle::getId).toList();

        // 배차 추천 로직 실행
        OptimizationResult optimizationResult = dispatchOptimizer.solve(candidates, orderIds, vehicleIds);

        List<DispatchRecommendationResponse.Item> items = optimizationResult.assignments().stream()
                .map(assignment -> new DispatchRecommendationResponse.Item(
                        assignment.orderId(),
                        orderById.get(assignment.orderId()).getOrderNo(),
                        assignment.vehicleId(),
                        vehicleById.get(assignment.vehicleId()).getVehicleNo(),
                        assignment.distanceKm()
                )).toList();

        return new DispatchRecommendationResponse(
                items,
                optimizationResult.unassignedOrderIds(),
                optimizationResult.unassignedVehicleIds()
        );
    }
}
