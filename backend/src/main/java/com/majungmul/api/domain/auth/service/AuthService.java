package com.majungmul.api.domain.auth.service;

import com.majungmul.api.domain.auth.dto.AnonymousLoginRequest;
import com.majungmul.api.domain.auth.dto.TokenResponse;
import com.majungmul.api.domain.auth.entity.RefreshToken;
import com.majungmul.api.domain.auth.repository.RefreshTokenRepository;
import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.repository.UserRepository;
import com.majungmul.api.global.common.exception.BusinessException;
import com.majungmul.api.global.common.exception.ErrorCode;
import com.majungmul.api.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증(Auth) 비즈니스 로직 서비스.
 *
 * <p>담당 기능:
 * <ol>
 *   <li>익명 로그인: deviceId로 사용자를 찾거나 신규 생성 후 JWT 발급</li>
 *   <li>토큰 재발급: 유효한 리프레시 토큰을 검증하고 새 토큰 쌍 발급</li>
 *   <li>로그아웃: 리프레시 토큰 DB에서 삭제</li>
 * </ol>
 *
 * <p>간이역 서비스 가치 관점:
 * 고립 청년들은 '또 다른 가입 절차'에서 느끼는 피로감으로 서비스를 이탈한다.
 * 기기 ID 하나만으로 즉시 로그인되는 이 흐름이 서비스 진입 장벽을 최소화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry; // 밀리초

    // ─────────────────────────────────────────────────────────────────
    // 익명 로그인
    // ─────────────────────────────────────────────────────────────────

    /**
     * 기기 ID 기반 익명 로그인을 처리한다.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. deviceId로 기존 사용자 조회
     * 2-a. [기존 사용자] 그대로 토큰 발급 → isNewUser = false
     * 2-b. [신규 사용자] ANONYMOUS 계정 생성 → 토큰 발급 → isNewUser = true
     * 3. 리프레시 토큰 DB 저장 (기존 토큰은 rotate)
     * 4. TokenResponse 반환
     * </pre>
     *
     * <p>⚠️ 익명성 보호: 이 메서드에서 deviceId를 로그에 출력하지 않음.
     *    로그에는 userId(PK)도 최소화. isNewUser 여부와 role 정도만 기록.
     *
     * @param request 기기 ID를 담은 요청 DTO
     * @return 액세스 토큰·리프레시 토큰·신규 여부를 담은 응답
     */
    @Transactional
    public TokenResponse anonymousLogin(AnonymousLoginRequest request) {

        // ① 기기 ID로 기존 사용자 여부 확인 — Repository 메서드로 단순 체크
        boolean isNewUser = !userRepository.existsByDeviceIdAndIsDeletedFalse(request.deviceId());

        User user = userRepository.findByDeviceIdAndIsDeletedFalse(request.deviceId())
                .orElseGet(() -> {
                    // 신규 익명 사용자 생성 — 이름·이메일 없이 기기 ID만으로 가입 완료
                    User newUser = User.createAnonymous(request.deviceId());
                    User saved = userRepository.save(newUser);
                    log.info("[Auth] 신규 익명 사용자 생성. role={}", saved.getRole()); // userId 로그 제외
                    return saved;
                });

        // ② JWT 토큰 발급
        String accessToken  = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        // ③ 리프레시 토큰 저장 (기존 토큰 있으면 rotate, 없으면 신규 저장)
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiry / 1000);

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        existing -> existing.rotate(refreshToken, expiresAt),
                        () -> refreshTokenRepository.save(
                                RefreshToken.create(user.getId(), refreshToken, expiresAt))
                );

        log.info("[Auth] 로그인 완료. role={}, isNewUser={}", user.getRole(), isNewUser);

        return TokenResponse.of(accessToken, refreshTokenExpiry, refreshToken, isNewUser);
    }

    // ─────────────────────────────────────────────────────────────────
    // 토큰 재발급
    // ─────────────────────────────────────────────────────────────────

    /**
     * 리프레시 토큰으로 새 액세스·리프레시 토큰을 재발급한다 (Token Rotation).
     *
     * <p>Token Rotation 전략:
     * 재발급 시마다 리프레시 토큰도 새 값으로 교체한다.
     * 기존 리프레시 토큰이 탈취되더라도 한 번만 사용 가능하므로 피해를 최소화.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. 리프레시 토큰 서명·만료 검증 (JwtProvider)
     * 2. DB에서 토큰 존재 여부 확인 (revoke 여부 체크)
     * 3. 새 액세스·리프레시 토큰 발급
     * 4. DB 리프레시 토큰 rotate
     * </pre>
     *
     * @param refreshTokenValue 클라이언트가 보낸 리프레시 토큰 문자열
     * @return 새 토큰 쌍
     * @throws BusinessException TOKEN_INVALID — 토큰 검증 실패 또는 DB에 없는 토큰
     */
    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {

        // ① 토큰 서명·만료 검증
        if (!jwtProvider.validateToken(refreshTokenValue)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // ② DB에서 리프레시 토큰 조회 (revoke·탈취 감지)
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        Long userId = jwtProvider.getUserId(refreshTokenValue);

        // userId로 사용자 조회 — 탈퇴 계정이면 A001(인증 필요) 예외 발생
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // ③ 새 토큰 발급
        String newAccessToken  = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        // ④ 리프레시 토큰 rotate (기존 토큰 교체)
        LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiry / 1000);
        storedToken.rotate(newRefreshToken, newExpiresAt);

        log.info("[Auth] 토큰 재발급 완료. role={}", user.getRole());

        return TokenResponse.of(newAccessToken, refreshTokenExpiry, newRefreshToken, false);
    }

    // ─────────────────────────────────────────────────────────────────
    // 로그아웃
    // ─────────────────────────────────────────────────────────────────

    /**
     * 로그아웃 처리 — 해당 사용자의 리프레시 토큰을 DB에서 삭제한다.
     *
     * <p>액세스 토큰은 만료 시각까지 유효하지만, 짧은 유효기간(1시간)으로
     * 실질적 위험이 적다. 필요 시 Redis 블랙리스트 방식으로 강화 가능.
     *
     * @param userId 로그아웃할 사용자 PK (JwtAuthenticationFilter가 SecurityContext에서 제공)
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("[Auth] 로그아웃 처리 완료.");
    }

}
