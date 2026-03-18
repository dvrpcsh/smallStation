package com.majungmul.api.domain.mission.repository;

import com.majungmul.api.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 미션(Mission) 레포지토리.
 */
public interface MissionRepository extends JpaRepository<Mission, Long> {

    /**
     * 활성화된 미션 목록을 조회한다.
     * {@code GET /api/v1/missions} 에서 클라이언트에 노출할 미션만 반환.
     *
     * @return isActive=true 인 미션 목록
     */
    List<Mission> findAllByIsActiveTrue();
}
