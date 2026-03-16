package com.majungmul.api.global.jwt;

import com.majungmul.api.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 발급·파싱·검증을 담당하는 유틸리티 컴포넌트.
 *
 * <p>발급 토큰 종류:
 * <ul>
 *   <li><b>액세스 토큰</b> — API 요청 헤더에 포함. 유효기간 짧음(1시간).</li>
 *   <li><b>리프레시 토큰</b> — 액세스 토큰 만료 시 재발급에 사용. 유효기간 김(30일).
 *       DB에 저장하여 탈취 시 무효화 가능.</li>
 * </ul>
 *
 * <p>토큰 페이로드 구조:
 * <pre>
 * {
 *   "sub":  "123",          // 사용자 ID (PK)
 *   "role": "ANONYMOUS",    // 사용자 역할
 *   "iat":  1234567890,     // 발급 시각
 *   "exp":  1234571490      // 만료 시각
 * }
 * </pre>
 *
 * <p>⚠️ 익명성 보호: 토큰 클레임에 deviceId, 닉네임 등 식별 가능 정보를 포함하지 않는다.
 *    사용자 식별은 sub(userId)만 사용.
 */
@Slf4j
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    /**
     * 생성자 주입으로 서명 키와 만료 시간을 초기화한다.
     *
     * @param secret             application.yml의 jwt.secret (최소 32자)
     * @param accessTokenExpiry  액세스 토큰 유효기간 (밀리초)
     * @param refreshTokenExpiry 리프레시 토큰 유효기간 (밀리초)
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {

        // HMAC-SHA256 키 생성 — 시크릿이 32바이트 미만이면 예외 발생
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    // ─────────────────────────────────────────────────────────────────
    // 토큰 발급
    // ─────────────────────────────────────────────────────────────────

    /**
     * 액세스 토큰을 발급한다.
     *
     * <p>클라이언트는 이 토큰을 Authorization 헤더에 담아 API를 호출한다:
     * {@code Authorization: Bearer <token>}
     *
     * @param userId 사용자 PK
     * @param role   사용자 역할 (ANONYMOUS, USER 등)
     * @return 서명된 JWT 액세스 토큰 문자열
     */
    public String generateAccessToken(Long userId, UserRole role) {
        return buildToken(userId, role, accessTokenExpiry);
    }

    /**
     * 리프레시 토큰을 발급한다.
     *
     * <p>액세스 토큰 만료 시 이 토큰으로 재발급을 요청한다.
     * DB에 저장되며, 탈취 감지 시 서버에서 무효화 가능.
     *
     * @param userId 사용자 PK
     * @param role   사용자 역할
     * @return 서명된 JWT 리프레시 토큰 문자열
     */
    public String generateRefreshToken(Long userId, UserRole role) {
        return buildToken(userId, role, refreshTokenExpiry);
    }

    /**
     * 내부 토큰 생성 공통 로직.
     *
     * @param userId  사용자 PK (sub 클레임)
     * @param role    사용자 역할 (role 클레임)
     * @param expiry  유효기간 (밀리초)
     */
    private String buildToken(Long userId, UserRole role, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .subject(String.valueOf(userId))   // 사용자 식별 — PK만 포함 (익명성 보호)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────
    // 토큰 파싱 및 검증
    // ─────────────────────────────────────────────────────────────────

    /**
     * 토큰의 유효성을 검증한다.
     *
     * <p>JwtAuthenticationFilter에서 요청마다 호출된다.
     *
     * @param token 검증할 JWT 문자열
     * @return 유효하면 true, 만료·변조 등 문제가 있으면 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // ⚠️ 익명성 보호: 만료된 토큰의 sub(userId) 등을 로그에 출력하지 않음
            log.warn("[JWT] 만료된 토큰입니다.");
        } catch (JwtException e) {
            log.warn("[JWT] 유효하지 않은 토큰입니다. type={}", e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * 토큰에서 사용자 ID(PK)를 추출한다.
     *
     * @param token 유효성이 검증된 JWT 문자열
     * @return 사용자 PK (Long)
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 사용자 역할을 추출한다.
     *
     * @param token 유효성이 검증된 JWT 문자열
     * @return 사용자 역할 열거형
     */
    public UserRole getUserRole(String token) {
        String roleName = parseClaims(token).get(CLAIM_ROLE, String.class);
        return UserRole.valueOf(roleName);
    }

    /**
     * 토큰의 만료 시각을 반환한다 (리프레시 토큰 DB 저장 시 사용).
     *
     * @param token JWT 문자열
     * @return 만료 시각 (Date)
     */
    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * JWT를 파싱하여 Claims(페이로드)를 반환하는 내부 메서드.
     *
     * @throws JwtException 서명 불일치, 형식 오류, 만료 등
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
