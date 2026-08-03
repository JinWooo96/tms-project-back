package com.tmsproject.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 예외의 베이스 클래스.
 * Service, Entity 어디서든 "이 상황은 ErrorCode의 어떤 케이스에 해당하는가"만 판단해서
 * new BusinessException(ErrorCode.XXX)로 던지면, GlobalExceptionHandler가 나머지를 처리한다.
 *
 * RuntimeException을 상속하는 이유: 체크 예외(Checked Exception)로 만들면
 * 이 예외를 던질 수 있는 모든 메서드 시그니처에 throws를 붙여야 하고,
 * 호출하는 쪽에서도 매번 try-catch를 강제당한다. 비즈니스 규칙 위반은
 * 대부분 GlobalExceptionHandler가 한 곳에서 일괄 처리하면 되므로 언체크 예외가 더 적합하다.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
