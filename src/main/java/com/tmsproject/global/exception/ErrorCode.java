package com.tmsproject.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 명세서 4장 "에러 코드 전체 목록"을 코드로 옮긴 것.
 * 여기에 정의되지 않은 새로운 에러 상황이 생기면, 반드시 이 Enum에 항목을 추가한다.
 * (문서와 코드가 항상 같은 목록을 가리키도록 유지하는 것이 핵심)
 */
@Getter
public enum ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 운송주문을 찾을 수 없습니다."),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 차량을 찾을 수 없습니다."),
    DISPATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 배차 건을 찾을 수 없습니다."),

    ORDER_ALREADY_DISPATCHED(HttpStatus.CONFLICT, "이미 배차된 주문입니다."),
    VEHICLE_NOT_AVAILABLE(HttpStatus.CONFLICT, "배차 불가능한 차량 상태입니다."),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "차량의 중량 또는 부피 조건을 초과했습니다."),
    DISPATCH_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 배차입니다."),
    DUPLICATE_VEHICLE_NO(HttpStatus.CONFLICT, "이미 등록된 차량번호입니다."),
    VEHICLE_IN_USE(HttpStatus.CONFLICT, "배차 중인 차량은 삭제할 수 없습니다."),
    VEHICLE_HAS_DISPATCH_HISTORY(HttpStatus.CONFLICT, "배차 이력이 있는 차량은 삭제할 수 없습니다."),
    // v3: 경로에 속한 배차는 경로 단위로만 취소 가능 (차량 상태를 경로가 한 번만 책임지기 위함)
    DISPATCH_IN_ROUTE(HttpStatus.CONFLICT, "경로에 속한 배차는 경로 단위로만 취소할 수 있습니다."),
    // v3: 이미 취소된 경로를 다시 취소하는 경우 (다른 경로의 차량 상태가 잘못 복구되는 것을 방지)
    ROUTE_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 배차경로입니다."),
    // 낙관적 락 충돌: 같은 차량·주문을 다른 사용자가 먼저 변경한 경우 (화면에 그대로 노출되는 문구)
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 사용자가 먼저 변경했습니다. 새로고침 후 다시 시도해 주십시오."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    GEOCODING_FAILED(HttpStatus.NOT_FOUND, "주소를 찾지 못했습니다."),

    NO_PENDING_ORDERS(HttpStatus.NOT_FOUND, "미배차 주문을 찾을 수 없습니다."),

    NO_AVAILABLE_VEHICLES(HttpStatus.NOT_FOUND, "배차 가능한 차량을 찾을 수 없습니다."),

    ROUTE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "경로 계산 중 오류가 발생했습니다."),
    TIME_WINDOW_VIOLATED(HttpStatus.CONFLICT, "계산된 방문 순서로는 시간창을 지킬 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
