package com.majungmul.api.global.common.exception;

import com.majungmul.api.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 애플리케이션 전역 예외 처리기.
 *
 * <p>예외 처리 흐름:
 * <pre>
 * Controller → Service (throws BusinessException)
 *                  ↓
 *     GlobalExceptionHandler 포착
 *                  ↓
 *     ApiResponse.fail(errorCode) 생성
 *                  ↓
 *     적절한 HTTP 상태 코드와 함께 클라이언트 반환
 * </pre>
 *
 * <p>처리 우선순위 (위쪽일수록 구체적인 예외를 먼저 처리):
 * <ol>
 *   <li>MethodArgumentNotValidException — @Valid 유효성 검사 실패</li>
 *   <li>BusinessException — 비즈니스 규칙 위반</li>
 *   <li>그 외 RuntimeException / Exception — 예상치 못한 오류</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── @Valid / @Validated 유효성 검사 실패 ──────────────────────────

    /**
     * 요청 바디(@RequestBody) 유효성 검사 실패 시 처리한다.
     *
     * <p>각 필드 오류를 "필드명: 메시지" 형식으로 이어 붙여 상세 메시지로 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {

        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[Validation] {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, details));
    }

    /**
     * 폼 데이터(@ModelAttribute) 바인딩·유효성 검사 실패 시 처리한다.
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[Bind] {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, details));
    }

    // ── HTTP 수준 예외 ─────────────────────────────────────────────────

    /**
     * 지원하지 않는 HTTP 메서드 호출 시 처리한다 (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e) {

        log.warn("[Method] {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 지원하지 않는 미디어 타입(Content-Type) 요청 시 처리한다 (415).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e) {

        log.warn("[MediaType] {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    // ── 비즈니스 예외 ──────────────────────────────────────────────────

    /**
     * Service 계층에서 throw 한 BusinessException(및 하위 클래스)을 처리한다.
     *
     * <p>ErrorCode 에 정의된 HTTP 상태와 메시지를 그대로 응답에 반영한다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[Business] code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode, e.getMessage()));
    }

    // ── 예상치 못한 오류 ───────────────────────────────────────────────

    /**
     * 핸들러가 정의되지 않은 모든 예외를 최종 포착한다 (500).
     *
     * <p>스택 트레이스를 ERROR 레벨로 기록하되,
     * 클라이언트에는 내부 오류임만 알려 정보 노출을 최소화한다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Unhandled] {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
