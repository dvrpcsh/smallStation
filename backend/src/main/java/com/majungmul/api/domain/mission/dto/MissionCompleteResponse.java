package com.majungmul.api.domain.mission.dto;

import com.majungmul.api.domain.mission.entity.Mission;

/**
 * 미션 완료 처리 응답 DTO.
 *
 * <p>미션 완료 후 클라이언트에 지급된 포인트와 미션 정보를 반환한다.
 * {@code POST /api/v1/missions/{missionId}/complete} 응답에 사용된다.
 *
 * @param missionId      완료한 미션 PK
 * @param missionTitle   완료한 미션 제목
 * @param rewardedPoints 이번에 적립된 포인트
 * @param totalPoints    적립 후 사용자의 누적 포인트
 */
public record MissionCompleteResponse(
        Long missionId,
        String missionTitle,
        int rewardedPoints,
        int totalPoints
) {

    /**
     * 미션 완료 응답 DTO를 생성한다.
     *
     * @param mission      완료한 미션 엔티티
     * @param totalPoints  포인트 적립 후 사용자의 누적 총점
     * @return MissionCompleteResponse DTO
     */
    public static MissionCompleteResponse of(Mission mission, int totalPoints) {
        return new MissionCompleteResponse(
                mission.getId(),
                mission.getTitle(),
                mission.getRewardAmount(),
                totalPoints
        );
    }
}
