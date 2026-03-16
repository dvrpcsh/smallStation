package com.majungmul.api.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드 정의.
 *
 * <p>코드 네이밍 규칙:
 * <pre>
 *   C  → Common  (공통)
 *   A  → Auth    (인증·인가)
 *   U  → User    (회원)
 *   P  → Post    (게시글·커뮤니티)
 *   M  → Mission (미션 인증·완료 처리)
 *   R  → Reward  (포인트 적립·상품권 처리)
 *   G  → Guardian (가디언 시스템)
 * </pre>
 *
 * <p>각 에러 코드는 (HTTP 상태, 식별 코드, 메시지) 세 가지 정보를 가진다.
 * GlobalExceptionHandler 가 BusinessException 에서 이 값을 꺼내 ApiResponse 로 변환한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Common ────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE  (HttpStatus.BAD_REQUEST,           "C002", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND   (HttpStatus.NOT_FOUND,             "C003", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED   (HttpStatus.METHOD_NOT_ALLOWED,    "C004", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C005", "지원하지 않는 미디어 타입입니다."),

    // ── Auth ──────────────────────────────────────────────────────────
    UNAUTHORIZED         (HttpStatus.UNAUTHORIZED,  "A001", "인증이 필요합니다."),
    FORBIDDEN            (HttpStatus.FORBIDDEN,     "A002", "접근 권한이 없습니다."),
    TOKEN_EXPIRED        (HttpStatus.UNAUTHORIZED,  "A003", "토큰이 만료되었습니다."),
    TOKEN_INVALID        (HttpStatus.UNAUTHORIZED,  "A004", "유효하지 않은 토큰입니다."),

    // ── User ──────────────────────────────────────────────────────────
    USER_NOT_FOUND       (HttpStatus.NOT_FOUND,    "U001", "존재하지 않는 사용자입니다."),
    EMAIL_DUPLICATED     (HttpStatus.CONFLICT,     "U002", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATED  (HttpStatus.CONFLICT,     "U003", "이미 사용 중인 닉네임입니다."),

    // ── Post ──────────────────────────────────────────────────────────
    POST_NOT_FOUND       (HttpStatus.NOT_FOUND,    "P001", "존재하지 않는 게시글입니다."),
    /** AI 세이프티 필터 — 부정 키워드 감지 시 게시 차단 */
    POST_BLOCKED_BY_SAFETY(HttpStatus.BAD_REQUEST, "P002", "안전 정책에 의해 게시가 차단되었습니다."),
    /** 댓글 조회 실패 — 삭제되었거나 존재하지 않는 댓글 */
    COMMENT_NOT_FOUND    (HttpStatus.NOT_FOUND,    "P003", "존재하지 않는 댓글입니다."),

    // ── Mission ───────────────────────────────────────────────────────
    MISSION_NOT_FOUND        (HttpStatus.NOT_FOUND,    "M001", "존재하지 않는 미션입니다."),
    /** 인증 사진·파일이 요청에 포함되지 않은 경우 */
    MISSION_FILE_MISSING     (HttpStatus.BAD_REQUEST,  "M002", "미션 인증 파일이 누락되었습니다."),
    MISSION_ALREADY_DONE     (HttpStatus.CONFLICT,     "M003", "이미 완료한 미션입니다."),

    // ── Reward ────────────────────────────────────────────────────────
    /** 포인트 사용 요청 시 보유 잔액이 부족한 경우 */
    REWARD_INSUFFICIENT_BALANCE  (HttpStatus.BAD_REQUEST, "R001", "포인트 잔액이 부족합니다."),
    /** 상품권 코드가 존재하지 않거나 이미 사용된 경우 */
    REWARD_INVALID_VOUCHER       (HttpStatus.BAD_REQUEST, "R002", "유효하지 않은 상품권입니다."),
    REWARD_NOT_FOUND             (HttpStatus.NOT_FOUND,   "R003", "존재하지 않는 리워드입니다."),

    // ── Guardian ──────────────────────────────────────────────────────
    GUARDIAN_NOT_FOUND   (HttpStatus.NOT_FOUND,    "G001", "담당 가디언을 찾을 수 없습니다."),
    GUARDIAN_ALERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "G002", "가디언 알림 전송에 실패했습니다.");

    // ─────────────────────────────────────────────────────────────────
    private final HttpStatus httpStatus;
    /** 클라이언트가 분기 처리에 사용하는 고유 식별 코드 */
    private final String code;
    /** 사용자에게 표시할 메시지 */
    private final String message;
}
