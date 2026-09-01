package com.tmsproject.dispatch.entity;

import com.tmsproject.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배차경로(멀티드롭) 엔티티. 한 차량의 하나의 운행에 속한 여러 배차(Dispatch)를 묶는 상위 단위.
 * (DB 설계 문서 v3, 3장 참고)
 *
 * 설계 원칙: 차량 상태(AVAILABLE ↔ ASSIGNED)를 바꾸는 책임은 개별 Dispatch가 아니라
 * 이 DispatchRoute가 경로 전체에 대해 "딱 한 번"만 진다. (백엔드 아키텍처 문서 v3, 5장 참고)
 */
@Entity
@Getter
@Table(name = "dispatch_route")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispatchRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_status", nullable = false, length = 20)
    private RouteStatus routeStatus;

    @Column(name = "total_distance_km")
    private BigDecimal totalDistanceKm;

    @Column(name = "total_duration_min")
    private Integer totalDurationMin;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 이 경로에 속한 배차들. mappedBy="dispatchRoute" : 연관관계의 주인은 Dispatch 쪽(FK를 가진 쪽)이다.
    @OneToMany(mappedBy = "dispatchRoute", fetch = FetchType.LAZY)
    private List<Dispatch> dispatchList = new ArrayList<>();

    @Builder
    private DispatchRoute(Vehicle vehicle, BigDecimal totalDistanceKm, Integer totalDurationMin) {
        this.vehicle = vehicle;
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationMin = totalDurationMin;
        this.routeStatus = RouteStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 이 경로를 확정하면서, 소속될 배차들을 방문 순서대로 함께 확정한다.
     *
     * @param dispatchesInOrder   방문 순서대로 정렬된 Dispatch 목록 (인덱스 0이 1번째 방문지)
     * @param estimatedArrivalAts dispatchesInOrder와 같은 순서의 예상 도착 시각 목록
     */
    public void confirmWith(List<Dispatch> dispatchesInOrder, List<LocalDateTime> estimatedArrivalAts) {
        for (int i = 0; i < dispatchesInOrder.size(); i++) {
            dispatchesInOrder.get(i).confirmWithinRoute(this, i + 1, estimatedArrivalAts.get(i));
        }
        this.dispatchList.addAll(dispatchesInOrder);
        this.vehicle.markAsAssigned(); // 차량 상태 변경은 경로 전체에 대해 딱 한 번만
    }

    /**
     * 경로 전체를 취소한다. 소속된 모든 배차를 함께 취소하고, 차량 상태를 원상복구한다.
     * (1차 범위에서는 경로 안 특정 배차만 취소하는 "부분 취소"는 지원하지 않는다.)
     */
    public void cancel() {
        this.routeStatus = RouteStatus.CANCELLED;
        for (Dispatch dispatch : this.dispatchList) {
            dispatch.cancelWithinRoute();
        }
        this.vehicle.markAsAvailable(); // 차량 상태 복구도 경로 전체에 대해 딱 한 번만
    }
}
