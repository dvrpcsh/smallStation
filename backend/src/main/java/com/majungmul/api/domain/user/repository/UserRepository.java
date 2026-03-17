package com.majungmul.api.domain.user.repository;

import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * User 도메인 데이터 접근 계층.
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 복잡한 쿼리가 필요한 경우 @Query 또는 QueryDSL을 이 인터페이스에 추가한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 기기 ID로 활성 사용자를 조회한다.
     *
     * <p>익명 로그인 흐름:
     * 1. deviceId로 기존 사용자 조회
     * 2. 없으면 새 ANONYMOUS 사용자 생성 (AuthService에서 처리)
     *
     * @param deviceId 클라이언트 기기 고유 ID
     * @return 삭제되지 않은 사용자 (Optional)
     */
    Optional<User> findByDeviceIdAndIsDeletedFalse(String deviceId);

    /**
     * 닉네임 중복 여부를 확인한다.
     * 닉네임 등록 시 중복 검사에 사용 (U003 에러 방지).
     *
     * @param nickname 중복 확인할 닉네임
     */
    boolean existsByNicknameAndIsDeletedFalse(String nickname);

    /** 기기 ID 기반 사용자 존재 여부 확인 — 익명 로그인에서 신규/기존 분기에 사용 */
    boolean existsByDeviceIdAndIsDeletedFalse(String deviceId);

    /**
     * 특정 역할을 가진 활성 사용자 전체를 조회한다.
     *
     * <p>주요 사용처: {@code GuardianAlertListener}에서 GUARDIAN 역할 사용자 전체 조회.
     *
     * @param role      조회할 역할 (예: UserRole.GUARDIAN)
     * @return 해당 역할의 활성 사용자 목록
     */
    List<User> findAllByRoleAndIsDeletedFalse(UserRole role);
}
