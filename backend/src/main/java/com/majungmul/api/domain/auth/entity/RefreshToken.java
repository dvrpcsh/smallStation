package com.majungmul.api.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 저장 엔티티.
 *
 * <p>리프레시 토큰을 DB에 저장하는 이유:
 * <ul>
 *   <li>탈취된 토큰을 서버에서 즉시 무효화(revoke)할 수 있다.</li>
 *   <li>사용자가 "모든 기기에서 로그아웃" 요청 시 해당 userId의 토큰을 전부 삭제한다.</li>
 * </ul>
 *
 * <p>DB 테이블: {@code refresh_tokens}
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토큰 소유자의 사용자 PK — User 엔티티를 직접 참조하지 않고 ID만 보관 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** DB에 저장되는 리프레시 토큰 문자열 */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** 토큰 만료 시각 — 스케줄러가 만료된 레코드를 주기적으로 정리하는 데 사용 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 저장 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 새 리프레시 토큰 레코드를 생성한다.
     *
     * @param userId    사용자 PK
     * @param token     발급된 리프레시 토큰 문자열
     * @param expiresAt 만료 시각
     */
    public static RefreshToken create(Long userId, String token, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.userId = userId;
        refreshToken.token = token;
        refreshToken.expiresAt = expiresAt;
        refreshToken.createdAt = LocalDateTime.now();
        return refreshToken;
    }

    /**
     * 토큰 값을 새 토큰으로 교체한다 (재발급 시 기존 레코드 재활용).
     *
     * @param newToken     새로 발급된 토큰 문자열
     * @param newExpiresAt 새 만료 시각
     */
    public void rotate(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
    }
}
