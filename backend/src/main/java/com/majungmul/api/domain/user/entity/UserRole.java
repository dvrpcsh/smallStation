package com.majungmul.api.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * 서비스 내 사용자 역할(권한) 정의.
 *
 * <p>GrantedAuthority 구현으로 Spring Security FilterChain에서
 * 역할 기반 접근 제어(RBAC)에 직접 활용된다.
 *
 * <p>역할 계층:
 * <pre>
 *   ANONYMOUS  - 기기 ID 기반으로 자동 생성된 익명 사용자.
 *                서비스 첫 진입 시 절차 없이 즉시 사용 가능.
 *                → 고립 청년들이 '또 다른 가입 절차'에서 느끼는 피로감을 제거.
 *
 *   USER       - 닉네임 등 최소 정보를 등록한 일반 사용자.
 *                커뮤니티 참여, 미션 수행 등 전체 기능 이용 가능.
 *
 *   GUARDIAN   - 검증된 또래 상담사. 위기 알림 수신 및 상담 기능 보유.
 *
 *   ADMIN      - 서비스 운영자. 전체 기능 및 관리 콘솔 접근 가능.
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum UserRole implements GrantedAuthority {

    ANONYMOUS("익명 사용자"),
    USER     ("일반 사용자"),
    GUARDIAN ("가디언 상담사"),
    ADMIN    ("관리자");

    private final String description;

    /**
     * Spring Security가 권한 비교 시 사용하는 문자열.
     * "ROLE_" 접두사를 붙여 hasRole("ANONYMOUS") 표현식과 연동.
     */
    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
