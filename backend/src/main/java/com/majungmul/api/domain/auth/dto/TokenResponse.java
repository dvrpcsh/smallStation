package com.majungmul.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 발급 응답 DTO.
 *
 * <p>클라이언트는 accessToken을 Authorization 헤더에 담아 API를 호출하고,
 * accessToken 만료 시 refreshToken으로 /auth/refresh를 통해 재발급을 요청한다.
 *
 * <p>응답 예시:
 * <pre>
 * {
 *   "tokenType": "Bearer",
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "accessTokenExpiresIn": 3600000,
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "isNewUser": true
 * }
 * </pre>
 */
@Schema(description = "JWT 토큰 발급 응답")
public record TokenResponse(

        @Schema(description = "토큰 타입 (항상 'Bearer')", example = "Bearer")
        String tokenType,

        @Schema(description = "API 호출 시 사용하는 단기 액세스 토큰 (1시간 유효)")
        String accessToken,

        @Schema(description = "액세스 토큰 유효기간 (밀리초)", example = "3600000")
        long accessTokenExpiresIn,

        @Schema(description = "액세스 토큰 재발급에 사용하는 장기 리프레시 토큰 (30일 유효)")
        String refreshToken,

        @Schema(description = "최초 가입 여부 — true면 온보딩 화면으로 안내", example = "true")
        boolean isNewUser

) {
    /** 정적 팩토리 메서드 — 불변 레코드를 일관된 방식으로 생성 */
    public static TokenResponse of(String accessToken,
                                   long accessTokenExpiresIn,
                                   String refreshToken,
                                   boolean isNewUser) {
        return new TokenResponse("Bearer", accessToken, accessTokenExpiresIn, refreshToken, isNewUser);
    }
}
