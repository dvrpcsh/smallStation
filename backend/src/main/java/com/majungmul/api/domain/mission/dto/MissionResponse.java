package com.majungmul.api.domain.mission.dto;

import com.majungmul.api.domain.mission.entity.Mission;
import com.majungmul.api.domain.mission.entity.MissionCategory;

/**
 * 미션 목록 조회 응답 DTO.
 *
 * <p>엔티티를 컨트롤러 레이어에 직접 노출하지 않기 위해 사용한다.
 * {@code GET /api/v1/missions} 응답에 사용된다.
 *
 * @param id           미션 PK
 * @param title        미션 제목
 * @param description  미션 설명
 * @param rewardAmount 완료 시 지급 포인트
 * @param category     미션 카테고리 (LIFE / EMOTION / RELATION)
 */
public record MissionResponse(
        Long id,
        String title,
        String description,
        int rewardAmount,
        MissionCategory category
) {

    /**
     * Mission 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param mission 변환할 Mission 엔티티
     * @return MissionResponse DTO
     */
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getRewardAmount(),
                mission.getCategory()
        );
    }
}
