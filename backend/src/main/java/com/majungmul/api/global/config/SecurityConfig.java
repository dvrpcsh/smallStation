package com.majungmul.api.global.config;

import com.majungmul.api.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 — JWT 필터 기반 Stateless 인증.
 *
 * <p>인증 흐름:
 * <pre>
 * HTTP 요청
 *   → JwtAuthenticationFilter  (토큰 추출·검증 → SecurityContext 등록)
 *   → UsernamePasswordAuthenticationFilter (통과, 세션 미사용)
 *   → Controller 진입
 * </pre>
 *
 * <p>공개 경로 (토큰 없이 접근 가능):
 * <ul>
 *   <li>/api/v1/auth/**   — 익명 로그인, 토큰 재발급</li>
 *   <li>/swagger-ui/**    — API 문서 (개발 환경)</li>
 *   <li>/v3/api-docs/**   — OpenAPI 스펙</li>
 * </ul>
 *
 * <p>그 외 모든 경로는 유효한 JWT 액세스 토큰이 필요하다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Swagger·Auth 엔드포인트 — 토큰 없이 접근 가능한 공개 경로 */
    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",      // 익명 로그인, 토큰 재발급
            "/swagger-ui/**",       // Swagger UI
            "/swagger-ui.html",
            "/v3/api-docs/**",      // OpenAPI JSON
            "/actuator/health"      // 헬스체크
    };

    /**
     * HTTP 보안 필터 체인을 정의한다.
     *
     * <ul>
     *   <li>CSRF: REST API — 비활성화 (Stateless이므로 CSRF 토큰 불필요)</li>
     *   <li>Session: STATELESS — JWT로 상태 관리, 서버 세션 미사용</li>
     *   <li>필터 순서: JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()   // 공개 경로 — 인증 불필요
                .anyRequest().authenticated()               // 그 외 — 유효한 JWT 필요
            )
            // JWT 필터를 Spring Security의 기본 인증 필터 앞에 삽입
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화 인코더.
     * 현재는 익명 인증이라 직접 사용하지 않으나,
     * 향후 이메일 로그인 도입 시 활용.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
