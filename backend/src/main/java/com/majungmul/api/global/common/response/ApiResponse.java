package com.majungmul.api.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.majungmul.api.global.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 엔드포인트의 공통 응답 래퍼.
 *
 * <p>응답 구조:
 * <pre>
 * {
 *   "success": true,
 *   "code":    "200",
 *   "message": "요청이 성공적으로 처리되었습니다.",
 *   "data":    { ... }      // 실패 시 null → JSON 직렬화에서 제외
 * }
 * </pre>
 *
 * <p>사용 예:
 * <pre>
 * // 성공 (데이터 포함)
 * return ResponseEntity.ok(ApiResponse.success(userDto));
 *
 * // 성공 (데이터 없음 — 삭제 등)
 * return ResponseEntity.ok(ApiResponse.success());
 *
 * // 실패
 * return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
 * </pre>
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;

    // data 필드가 null이면 JSON 응답에서 생략 (응답 크기 최소화)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    // ────────────────────────────────────────────
    // 성공 팩토리 메서드
    // ────────────────────────────────────────────

    /**
     * 데이터를 포함한 성공 응답을 생성한다.
     *
     * @param data 응답 페이로드
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code    = "200";
        response.message = "요청이 성공적으로 처리되었습니다.";
        response.data    = data;
        return response;
    }

    /**
     * 메시지만 있는 성공 응답을 생성한다 (로그아웃·삭제 완료 등 data 불필요한 경우).
     *
     * @param message 클라이언트에 표시할 사용자 친화적 메시지
     */
    public static ApiResponse<Void> success(String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = true;
        response.code    = "200";
        response.message = message;
        return response;
    }

    /**
     * 데이터가 없는 성공 응답을 생성한다 (삭제·수정 완료 등).
     */
    public static ApiResponse<Void> success() {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = true;
        response.code    = "200";
        response.message = "요청이 성공적으로 처리되었습니다.";
        return response;
    }

    /**
     * 커스텀 메시지를 포함한 성공 응답을 생성한다.
     *
     * @param message 클라이언트에 표시할 사용자 친화적 메시지
     * @param data    응답 페이로드
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code    = "200";
        response.message = message;
        response.data    = data;
        return response;
    }

    // ────────────────────────────────────────────
    // 실패 팩토리 메서드
    // ────────────────────────────────────────────

    /**
     * ErrorCode 기반 실패 응답을 생성한다.
     *
     * <p>GlobalExceptionHandler 에서 BusinessException을 잡아 이 메서드로 변환한다.
     *
     * @param errorCode 정의된 에러 코드 열거형
     */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = false;
        response.code    = errorCode.getCode();
        response.message = errorCode.getMessage();
        return response;
    }

    /**
     * 커스텀 메시지를 포함한 실패 응답을 생성한다.
     *
     * @param errorCode 에러 코드
     * @param message   기본 메시지를 덮어쓸 상세 메시지
     */
    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = false;
        response.code    = errorCode.getCode();
        response.message = message;
        return response;
    }
}
