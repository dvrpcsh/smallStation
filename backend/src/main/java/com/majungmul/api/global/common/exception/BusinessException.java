package com.majungmul.api.global.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 모든 커스텀 예외의 기반 클래스.
 *
 * <p>사용 패턴:
 * <pre>
 * // Service 계층에서 직접 사용
 * throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 *
 * // 도메인별 예외 클래스 확장 (선택)
 * public class UserNotFoundException extends BusinessException {
 *     public UserNotFoundException() {
 *         super(ErrorCode.USER_NOT_FOUND);
 *     }
 * }
 * </pre>
 *
 * <p>GlobalExceptionHandler 가 이 예외를 잡아 ApiResponse.fail(errorCode) 로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 기본 에러 메시지(ErrorCode 에 정의된 메시지)로 예외를 생성한다.
     *
     * @param errorCode 발생한 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 상세 메시지를 직접 지정하여 예외를 생성한다.
     *
     * @param errorCode 발생한 에러 코드
     * @param message   기본 메시지를 덮어쓸 상세 설명
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 원인 예외를 체이닝하여 예외를 생성한다 (외부 API 호출 실패 등).
     *
     * @param errorCode 발생한 에러 코드
     * @param cause     원인이 된 예외
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
