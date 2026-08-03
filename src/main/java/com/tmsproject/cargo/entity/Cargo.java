package com.tmsproject.cargo.entity;


import com.tmsproject.order.entity.TransportOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 화물 엔티티. 하나의 운송주문(TransportOrder)에 여러 개가 속할 수 있다.
 */
@Entity
@Table(name = "cargo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne : Cargo 여러개가 하나의 TransportOrder에 속한다.
    // JoinColumn : 실제 FK 컬럼, 이 어노테이션이 transport_order_id 쪽으로 매핑된다.
    // 연관관계의 주인 (FK를 가진 쪽). FetchType.LAZY로 필요할 때만 조회하도록 설정.
    // FetchType.Lazy : Cargo를 조회할 때 즉시 order를 조회하지 말고 get으로 호출하는 시점에 DB조회를 해라
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_order_id", nullable = false)
    private TransportOrder transportOrder;

    @Column(name = "width_cm", nullable = false)
    private Integer widthCm;

    @Column(name = "length_cm", nullable = false)
    private Integer lengthCm;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    // double 타입은 오차가 발생할 수 있다. 중요한 소수점 계산은 BigDecimal 사용
    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_type", nullable = false, length = 20)
    private CargoType cargoType;

    // transportOrder 없음. 나중에 addCargo로 연관관계 생성
    @Builder
    private Cargo(Integer widthCm, Integer lengthCm, Integer heightCm,
                  BigDecimal weightKg, Integer quantity, CargoType cargoType) {
        this.widthCm = widthCm;
        this.lengthCm = lengthCm;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.quantity = quantity;
        this.cargoType = cargoType;
    }

    /** TransportOrder.addCargo()에서만 호출되는 패키지 내부용 메서드 (양방향 연관관계 편의 메서드)
     * --> 일단 public 으로 선언
     * */
    public void assignTransportOrder(TransportOrder transportOrder) {
        this.transportOrder = transportOrder;
    }

    /**
     * 화물의 총 중량, 부피를 구하는 것은 Cargo 자신의 데이터로 가능한 일
     * 그렇기 때문에 Service에서 하지 않고, 엔티티 스스로 책임지도록 설계
     * -> 객체지향 원칙과 맞음
     */

    /** 이 화물 한 줄의 총중량 (중량 × 수량 개념) */
    public BigDecimal totalWeight() {
        return weightKg.multiply(BigDecimal.valueOf(quantity));
    }

    /** 이 화물 한 줄의 총부피 (가로×세로×높이×수량) */
    public long totalVolume() {
        return (long) widthCm * lengthCm * heightCm * quantity;
    }
}