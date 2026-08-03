package com.tmsproject.cargo.entity;

/**
 * 화물 유형
 * 1차 범위에서는 저장만 하고 배차 로직에는 반영하지 않는다.
 * (추후 차량 매칭 제약조건으로 확장 예정: 예) FROZEN 화물은 냉동탑차만 배차 가능)
 */
public enum CargoType {
    GENERAL,    // 일반
    FROZEN,     // 냉동
    DANGEROUS   // 위험물
}
