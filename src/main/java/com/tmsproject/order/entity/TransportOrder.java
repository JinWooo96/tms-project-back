package com.tmsproject.order.entity;


import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.global.domain.BaseEntity;
import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
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
 * 운송주문 엔티티.
 * 화주가 등록하는 운송 요청 단위이며, 하나의 주문에 여러 화물(Cargo)이 포함될 수 있다.
 *
 * 엔티티 설계 규칙 (도메인 주도 설계 DDD)
 * - 상태를 마음대로 바꿀 수 없고, 메서드를 통해서만 강제한다. (setter 사용하지 않음)
 * - 상태가 왜 바뀌었는지 쉽게 추적 가능
 */
@Entity
@Table(name = "transport_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자가 필요하지만, 외부에서 new로 막 만들지 못하게 protected로 제한
public class TransportOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @Column(name = "origin_address", nullable = false)
    private String originAddress;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lng")
    private Double originLng;

    @Column(name = "dest_address", nullable = false)
    private String destAddress;

    @Column(name = "dest_lat")
    private Double destLat;

    @Column(name = "dest_lng")
    private Double destLng;

    @Column(name = "requested_pickup_at")
    private LocalDateTime requestedPickupAt;

    @Column(name = "requested_delivery_at")
    private LocalDateTime requestedDeliveryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    // OnToMany : TransportOrder 하나에 여러 Cargo가 연결된다. (1 : N) 관계 설정
    // mappedBy = "transportOrder" : 연관관계의 주인은 Cargo 쪽(FK를 가진 쪽)이라는 의미.
    // cascade = ALL : 주문을 저장/삭제하면 화물 리스트도 함께 저장/삭제된다.
    // orphanRemoval = true : cargoList에서 항목을 제거하면 DB에서도 삭제된다.
    // 기본값 lazy 조회 : 처음 조회할 때는 sql을 호출하지 않고, get 호출이 있을 때 sql 쿼리문(SELECT * FROM cargo)을 조회한다.
    @OneToMany(mappedBy = "transportOrder", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Cargo> cargoList = new ArrayList<>();

    /**
     * 생성자를 막아두었기 때문에 외부에서는 오직 Builder를 통해서 엔티티를 생성할 수 있다.
     */
    @Builder
    private TransportOrder(String orderNo, String originAddress, Double originLat, Double originLng,
                           String destAddress, Double destLat, Double destLng,
                           LocalDateTime requestedPickupAt, LocalDateTime requestedDeliveryAt) {
//        this.orderNo = orderNo;
        this.originAddress = originAddress;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destAddress = destAddress;
        this.destLat = destLat;
        this.destLng = destLng;
        this.requestedPickupAt = requestedPickupAt;
        this.requestedDeliveryAt = requestedDeliveryAt;
        this.status = OrderStatus.RECEIVED; // 생성 시점엔 항상 접수 상태로 시작
    }

    /**
     * 서버 생성 규칙에 따라 만들어진 주문번호를 할당한다.
     * TransportOrderService.registerOrder()에서 저장 직전에만 호출한다.
     */
    public void assignOrderNo(String orderNo) {
        if (this.orderNo != null) {
            // 개발자가 코드를 잘못 짯을 때만 발생하는 예외
            // 그래서 ErrorCode에 넣지 않는다.
            throw new IllegalStateException("주문번호는 이미 할당되어 있어 재할당할 수 없습니다.");
        }
        this.orderNo = orderNo;
    }

    /**
     * 화물을 주문에 추가한다. 양방향 연관관계이므로 Cargo 쪽에도 주문을 세팅해준다.
     */
    public void addCargo(Cargo cargo) {
        this.cargoList.add(cargo);
        cargo.assignTransportOrder(this);
    }

    /**
     * 배차가 확정되었을 때 호출 (Dispatch 도메인에서 호출)
     * 비즈니스 규칙을 메서드 안에 캡슐화
     *
     * - 엄격한 DDD 관점에서 보면 엔티티가 global.exception 패키지에 의존하는 것은 논쟁이 있을 수 있다.
     * 현재 프로젝트 규모에서는 ErrorCode가 사실상 "우리 시스템의 비즈니스 규칙 목록" 그 자체를 표현하는 거라
     * 엔티티가 이를 참조하는 것이 자연스럽다.
     * 나중에 서비스 규모가 커지면 "도메인 전용 예외"를 엔티티가 던지고, 그걸 GlobalExceptionHandler가 ErrorCode로 매핑" 하는 식으로 분리할 수도 있다.
     */
    public void markAsDispatched() {
        if (this.status != OrderStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_DISPATCHED);
        }
        this.status = OrderStatus.DISPATCHED;
    }

    /**
     * 배차가 취소되었을 때 호출 (다시 접수 상태로 되돌림)
     */
    public void revertToReceived() {
        this.status = OrderStatus.RECEIVED;
    }

    /**
     * 이 주문에 속한 모든 화물의 총중량 합계.
     * VehicleService/DispatchService 양쪽에서 중복 계산하지 않도록 여기서 한 번만 계산한다.
     */
    public BigDecimal totalWeight() {
        return cargoList.stream()
                .map(Cargo::totalWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 이 주문에 속한 모든 화물의 총부피 합계. */
    public long totalVolume() {
        return cargoList.stream()
                .mapToLong(Cargo::totalVolume)
                .sum();
    }
}