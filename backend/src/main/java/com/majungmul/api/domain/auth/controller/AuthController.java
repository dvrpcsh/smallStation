package com.majungmul.api.domain.auth.controller;

import com.majungmul.api.domain.auth.dto.AnonymousLoginRequest;
import com.majungmul.api.domain.auth.dto.TokenResponse;
import com.majungmul.api.domain.auth.service.AuthService;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증(Auth) API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>POST /api/v1/auth/anonymous — 기기 ID 기반 익명 로그인</li>
 *   <li>POST /api/v1/auth/refresh   — 리프레시 토큰으로 토큰 재발급</li>
 *   <li>POST /api/v1/auth/logout    — 로그아웃 (리프레시 토큰 무효화)</li>
 * </ul>
 *
 * <p>모든 응답은 {@link ApiResponse} 규격을 따른다.
 */
@Tag(name = "Auth", description = "익명 인증 및 토큰 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/anonymous
    // ─────────────────────────────────────────────────────────────────

    /**
     * 익명 로그인 API.
     *
     * <p>간이역 서비스 핵심 가치: 이름, 이메일, 비밀번호 없이 기기 ID 하나만으로
     * 즉시 서비스 이용이 가능하다. 고립 청년들이 '가입 절차'에서 느끼는 피로 없이
     * 바로 안전한 공간에 진입할 수 있도록 설계했다.
     *
     * <p>응답의 isNewUser가 true이면, 클라이언트는 온보딩 화면을 표시한다.
     */
    @Operation(
        summary = "익명 로그인",
        description = """
            기기 고유 ID(deviceId)만으로 로그인합니다.
            - 기존 사용자: 즉시 토큰 발급
            - 신규 사용자: 익명 계정 자동 생성 후 토큰 발급
            - isNewUser=true이면 클라이언트에서 온보딩 화면 표시
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류 (deviceId 누락 등)")
    })
    @PostMapping("/anonymous")
    public ResponseEntity<ApiResponse<TokenResponse>> anonymousLogin(
            @Valid @RequestBody AnonymousLoginRequest request) {

        TokenResponse tokenResponse = authService.anonymousLogin(request);
        return ResponseEntity.ok(ApiResponse.success(
                tokenResponse.isNewUser() ? "환영합니다! 간이역에 오신 걸 축하해요." : "다시 오셨군요, 반갑습니다.",
                tokenResponse));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/refresh
    // ─────────────────────────────────────────────────────────────────

    /**
     * 액세스 토큰 재발급 API.
     *
     * <p>액세스 토큰 만료(401) 시 클라이언트는 저장된 리프레시 토큰으로 이 API를 호출한다.
     * Token Rotation 전략으로 리프레시 토큰도 함께 교체되어 보안을 강화한다.
     */
    @Operation(
        summary = "액세스 토큰 재발급",
        description = """
            리프레시 토큰으로 새 액세스·리프레시 토큰을 발급합니다 (Token Rotation).
            - 요청 헤더: Authorization: Bearer {refreshToken}
            - 성공 시 기존 리프레시 토큰은 무효화되고 새 토큰이 발급됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Parameter(description = "리프레시 토큰 (Authorization: Bearer {token})")
            @RequestHeader("Authorization") String authorizationHeader) {

        // "Bearer " 접두사 제거
        String refreshToken = authorizationHeader.substring(7);
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/logout
    // ─────────────────────────────────────────────────────────────────

    /**
     * 로그아웃 API.
     *
     * <p>서버에 저장된 리프레시 토큰을 삭제하여 해당 기기의 자동 로그인을 차단한다.
     * 액세스 토큰은 만료(1시간)까지 유효하나, 짧은 유효기간으로 위험이 최소화된다.
     */
    @Operation(
        summary = "로그아웃",
        description = "현재 기기의 리프레시 토큰을 무효화합니다. 재로그인 시 새 토큰 발급 필요."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }
}
