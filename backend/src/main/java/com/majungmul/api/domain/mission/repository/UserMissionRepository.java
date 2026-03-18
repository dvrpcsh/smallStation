package com.majungmul.api.domain.mission.repository;

import com.majungmul.api.domain.mission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * 미션 완료 인증 로그(UserMission) 레포지토리.
 */
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

    /**
     * 특정 사용자가 특정 날짜에 특정 미션을 이미 완료했는지 확인한다.
     *
     * <p>하루 1회 제한 구현을 위해 {@code MissionService.completeMission()} 에서 호출된다.
     *
     * @param userId        확인할 사용자 PK
     * @param missionId     확인할 미션 PK
     * @param completedDate 확인할 날짜 (오늘)
     * @return 이미 완료 기록이 존재하면 true
     */
    boolean existsByUserIdAndMissionIdAndCompletedDate(Long userId, Long missionId, LocalDate completedDate);
}
