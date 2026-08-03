package com.tmsproject.global.response;

import com.tmsproject.global.exception.ErrorCode;

/**
 * ApiResponse.error에 담기는 실제 에러 정보. (API 명세서 "실패 시" 응답 형식 참고)
 * { "code": "ORDER_NOT_FOUND", "message": "해당 운송주문을 찾을 수 없습니다." }
 */
public record ErrorResponse(String code, String message) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
