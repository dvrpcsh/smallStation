package com.majungmul.api.domain.auth.repository;

import com.majungmul.api.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * RefreshToken 데이터 접근 계층.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 문자열로 리프레시 토큰 레코드를 조회한다.
     * 재발급(/auth/refresh) 요청 시 클라이언트가 보낸 토큰 검증에 사용.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 PK로 리프레시 토큰을 조회한다.
     * 동일 사용자의 기존 토큰을 교체(rotate)하거나 로그아웃 시 삭제에 사용.
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * 사용자 PK에 해당하는 모든 리프레시 토큰을 삭제한다.
     * "모든 기기에서 로그아웃" 기능에 사용.
     */
    void deleteAllByUserId(Long userId);
}
