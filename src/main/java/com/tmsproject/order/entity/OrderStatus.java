package com.tmsproject.order.entity;

/**
 * 운송주문 상태
 * RECEIVED(접수) → DISPATCHED(배차완료) → IN_TRANSIT(운송중) → COMPLETED(완료)
 */
public enum OrderStatus {
    RECEIVED,
    DISPATCHED,
    IN_TRANSIT,
    COMPLETED
}
