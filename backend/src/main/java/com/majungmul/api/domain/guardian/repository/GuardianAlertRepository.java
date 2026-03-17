package com.majungmul.api.domain.guardian.repository;

import com.majungmul.api.domain.guardian.entity.GuardianAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 가디언 알림 데이터 접근 계층.
 */
public interface GuardianAlertRepository extends JpaRepository<GuardianAlert, Long> {

    /**
     * 특정 가디언의 알림 목록을 최신순으로 조회한다.
     *
     * @param guardianId 알림을 조회할 가디언 userId
     * @return 가디언에게 발송된 알림 목록 (최신순)
     */
    List<GuardianAlert> findAllByGuardianIdOrderByCreatedAtDesc(Long guardianId);
}
