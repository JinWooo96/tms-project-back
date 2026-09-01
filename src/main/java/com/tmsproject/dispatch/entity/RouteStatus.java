package com.tmsproject.dispatch.entity;

/**
 * 배차경로(DispatchRoute) 상태
 * CONFIRMED(확정) → CANCELLED(취소)
 * 부분 취소는 v3 범위에서 지원하지 않으므로, PENDING 상태 없이 확정 시점에 바로 CONFIRMED로 생성된다.
 */
public enum RouteStatus {
    CONFIRMED,
    CANCELLED
}
