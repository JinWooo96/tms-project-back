package com.tmsproject.global.exception;

import com.tmsproject.global.response.ApiResponse;
import com.tmsproject.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 Controller에서 발생하는 예외를 한 곳에서 처리한다.
 * @RestControllerAdvice는 @ControllerAdvice + @ResponseBody가 합쳐진 것으로,
 * 모든 @RestController에서 발생하는 예외를 가로채서 여기 정의된 메서드로 위임한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 규칙 위반 (ErrorCode에 정의된 상황들).
     * ErrorCode 자체가 HTTP 상태코드를 갖고 있으므로, 여기서는 그걸 그대로 꺼내 쓰기만 하면 된다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException : {} - {}", errorCode.name(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(ErrorResponse.of(errorCode)));
    }

    /**
     * @Valid 검증 실패 시 Spring이 던지는 예외.
     * 여러 필드가 동시에 실패할 수 있는데, 지금은 가장 먼저 발견된 오류 메시지 하나만 반환한다.
     * (여러 개를 한 번에 다 보여주고 싶으면 getFieldErrors() 전체를 순회해서 리스트로 만들면 된다)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        log.warn("ValidationException: {}", message);

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), message)));
    }

    /**
     * 위에서 처리하지 못한 나머지 모든 예외 (예상 못한 버그, NPE 등).
     * 이 경우엔 내부 정보(스택트레이스 등)를 그대로 클라이언트에 노출하지 않고,
     * 서버 로그에만 자세히 남긴 뒤 클라이언트에는 일반적인 메시지만 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception occurred", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)));
    }
}
