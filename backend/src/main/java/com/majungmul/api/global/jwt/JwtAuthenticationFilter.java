package com.majungmul.api.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 기반 인증 필터.
 *
 * <p>모든 HTTP 요청에서 단 한 번만 실행되며(OncePerRequestFilter),
 * Authorization 헤더의 Bearer 토큰을 추출·검증하여 SecurityContext에 인증 정보를 설정한다.
 *
 * <p>처리 흐름:
 * <pre>
 * HTTP 요청
 *   → extractToken()     — "Authorization: Bearer xxx" 헤더에서 토큰 파싱
 *   → jwtProvider.validateToken()  — 서명·만료 검증
 *   → SecurityContext 등록  — 이후 컨트롤러에서 @AuthenticationPrincipal로 userId 접근 가능
 *   → FilterChain 통과
 * </pre>
 *
 * <p>토큰이 없거나 유효하지 않으면 SecurityContext를 비워둔 채 다음 필터로 넘긴다.
 * 인증이 필요한 엔드포인트는 SecurityConfig의 authorizeHttpRequests에서 접근 차단.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // 토큰이 존재하고 유효한 경우에만 SecurityContext에 인증 정보 등록
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            setAuthentication(token);
        }

        // 토큰 유무와 관계없이 다음 필터 체인으로 진행
        // (인증 필요 여부는 SecurityConfig의 authorizeHttpRequests에서 결정)
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 Bearer 토큰을 추출한다.
     *
     * @param request HTTP 요청
     * @return 토큰 문자열, 없으면 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 검증된 토큰으로 SecurityContext에 인증 정보를 등록한다.
     *
     * <p>UsernamePasswordAuthenticationToken의 principal에 userId(Long)를 저장하므로
     * 컨트롤러에서 @AuthenticationPrincipal Long userId 형태로 바로 꺼낼 수 있다.
     *
     * <p>⚠️ 익명성 보호: userId(PK)만 SecurityContext에 저장. deviceId·닉네임 등은 포함하지 않음.
     *
     * @param token 유효성이 검증된 JWT 토큰
     */
    private void setAuthentication(String token) {
        Long userId = jwtProvider.getUserId(token);
        String roleAuthority = jwtProvider.getUserRole(token).getAuthority(); // "ROLE_ANONYMOUS" 등

        // ⚠️ 익명성 보호: userId를 로그에 남기지 않음 (PK도 연속적 추측 가능)
        log.debug("[JWT] 인증 처리 완료. role={}", roleAuthority);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,                                       // principal: Long userId
                        null,                                         // credentials: 불필요
                        List.of(new SimpleGrantedAuthority(roleAuthority)) // authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
