package com.tmsproject.dispatch.entity;

/**
 * 배차 상태
 * PENDING(배차대기) → CONFIRMED(배차확정) → CANCELLED(배차취소)
 */
public enum DispatchStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}