package com.tmsproject.dispatch.entity;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "dispatch")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Dispatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계 필드
    // 반대편에 dispatchList 이런건 아직 만들지 않음.
    // 주문 하나에 주문이력 여러개가 딸려있는 걸 조회할 일이 아직 없어서 뺌.
    // 나중에 추가하면 된다. 지금은 괜히 순환참조가 생길 수도 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_order_id", nullable = false)
    private TransportOrder transportOrder;

    // 연관관계 필드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // v3 신규: 이 배차가 속한 멀티드롭 경로. 수동 단건 배차는 NULL로 남는다.
    // (DispatchRoute 쪽에서 dispatchList를 mappedBy로 참조하므로, 연관관계의 주인은 여기(Dispatch)다.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_route_id")
    private DispatchRoute dispatchRoute;

    // 1차 범위에서는 기사 관리를 하지 않으므로 FK 매핑 없이 ID만 nullable로 보관 (추후 Driver 엔티티 생기면 연관관계로 교체)
    @Column(name = "driver_id")
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 20)
    private DispatchStatus dispatchStatus;

    // 추후 경로 최적화 확장용. 1차에서는 사용하지 않음 (항상 null)
    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // v3 신규: 이 배차(주문)의 예상 도착(방문) 일시. 경로 확정 시 계산되어 저장된다.
    @Column(name = "estimated_arrival_at")
    private LocalDateTime estimatedArrivalAt;

    // 상속 받지 않고 직접 선언
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Dispatch(TransportOrder transportOrder, Vehicle vehicle) {
        this.transportOrder = transportOrder;
        this.vehicle = vehicle;
        this.dispatchStatus = DispatchStatus.PENDING; // pending으로 시작
    }

    /**
     * 배차를 확정한다. 연관된 주문/차량의 상태도 함께 변경한다.
     * (도메인 규칙: 배차 확정은 반드시 주문/차량 상태 변경과 함께 원자적으로 일어나야 하므로
     *  이 메서드 하나로 묶어서 Service에서 실수로 한쪽만 바꾸는 걸 방지한다.)
     */
    public void confirm() {
        this.dispatchStatus = DispatchStatus.CONFIRMED;
        this.assignedAt = LocalDateTime.now();
        this.transportOrder.markAsDispatched();
        this.vehicle.markAsAssigned();
    }

    /**
     * 배차를 취소한다. 연관된 주문/차량 상태를 원상복구한다.
     */
    public void cancel() {
        if (this.dispatchStatus == DispatchStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.DISPATCH_ALREADY_CANCELLED);
        }
        this.dispatchStatus = DispatchStatus.CANCELLED;
        this.transportOrder.revertToReceived();
        this.vehicle.markAsAvailable();
    }

    /**
     * [v3 신규 - 경로 전용] 이 배차를 특정 경로(DispatchRoute)에 속한 배차로 확정한다.
     *
     * confirm()과의 차이: 여기서는 vehicle.markAsAssigned()를 호출하지 않는다.
     * 멀티드롭 경로에서는 한 차량이 여러 주문(=여러 Dispatch)을 동시에 맡으므로,
     * 차량 상태 변경은 개별 배차가 아니라 DispatchRoute가 "경로 전체에 대해 딱 한 번"만 책임진다.
     * 이 메서드는 DispatchRoute.confirmWith() 내부에서만 호출된다.
     *
     * @param route               이 배차가 속하는 경로
     * @param sequence            이 경로 안에서의 방문 순서 (1부터 시작)
     * @param estimatedArrivalAt  계산된 예상 도착(방문) 일시
     */
    public void confirmWithinRoute(DispatchRoute route, int sequence, LocalDateTime estimatedArrivalAt) {
        this.dispatchRoute = route;
        this.sequence = sequence;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.dispatchStatus = DispatchStatus.CONFIRMED;
        this.assignedAt = LocalDateTime.now();
        this.transportOrder.markAsDispatched(); // 주문 상태 변경은 배차마다 각각 필요하다
    }

    /**
     * [v3 신규 - 경로 전용] 이 배차를 경로 취소의 일부로 취소한다.
     *
     * cancel()과의 차이: 여기서는 vehicle.markAsAvailable()를 호출하지 않는다.
     * 차량 상태 복구도 DispatchRoute가 경로 전체에 대해 한 번만 책임진다.
     * 이 메서드는 DispatchRoute.cancel() 내부에서만 호출된다.
     */
    public void cancelWithinRoute() {
        this.dispatchStatus = DispatchStatus.CANCELLED;
        this.transportOrder.revertToReceived();
    }
}
