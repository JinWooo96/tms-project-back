package com.tmsproject.global.response;

import lombok.Getter;

/**
 * 모든 API가 이 형태로 응답한다. (API 명세서 "공통 응답 포맷" 참고)
 * {
 *   "success": true,
 *   "data": { ... },
 *   "error": null
 * }
 * 실패 시에는 data가 null이 되고 error에 ErrorResponse(code, message)가 채워진다.
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

}
