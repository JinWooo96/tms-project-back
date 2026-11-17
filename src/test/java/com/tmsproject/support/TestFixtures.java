package com.tmsproject.support;

import com.tmsproject.cargo.entity.Cargo;
import com.tmsproject.cargo.entity.CargoType;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.vehicle.entity.Vehicle;

import java.math.BigDecimal;

/**
 * 테스트용 엔티티를 만드는 공장(Test Fixture).
 * 픽스쳐 : 테스트에서 반복해서 쓰는 준비물(차량, 주문)을 만드는 공장
 * 여러 테스트가 같은 준비물을 쓰므로 한곳에 모아 중복을 줄이고,
 * 테스트 본문에는 "무엇을 검증하는지"만 남긴다.
 */
public final class TestFixtures {

    private TestFixtures() {
        // 유틸리티 클래스이므로 인스턴스 생성 금지
    }

    /** 1톤 차량: 최대 1,000kg, 적재함 160 x 280 x 170cm (부피 7,616,000cm³) */
    public static Vehicle vehicle1Ton(String vehicleNo) {
        return Vehicle.builder()
                .vehicleNo(vehicleNo)
                .vehicleType("1TON")
                .maxWeightKg(new BigDecimal("1000.00"))
                .bedWidthCm(160)
                .bedLengthCm(280)
                .bedHeightCm(170)
                .build();
    }

    /** 화물 1개짜리 운송주문 (중량과 크기를 지정) */
    public static TransportOrder order(BigDecimal weightKg, int widthCm, int lengthCm, int heightCm) {
        TransportOrder order = TransportOrder.builder()
                .originAddress("테스트 상차지")
                .destAddress("테스트 하차지")
                .build();
        order.addCargo(Cargo.builder()
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .weightKg(weightKg)
                .quantity(1)
                .cargoType(CargoType.GENERAL)
                .build());
        return order;
    }

    /** 기본 운송주문: 100kg, 100 x 100 x 100cm */
    public static TransportOrder order() {
        return order(new BigDecimal("100.00"), 100, 100, 100);
    }
}
