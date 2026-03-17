package com.majungmul.api.domain.guardian.dto;

import com.majungmul.api.domain.guardian.entity.GuardianAlert;
import com.majungmul.api.domain.guardian.event.CrisisSourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 가디언 알림 응답 DTO.
 *
 * <p>⚠️ 익명성 보호: triggerUserId(PK)만 포함하며 닉네임·deviceId 등은 절대 포함하지 않는다.
 * 가디언은 PK를 통해 내부 관리 도구에서만 추가 조회가 가능하도록 설계한다.
 *
 * @param id            알림 PK
 * @param triggerUserId 위기 콘텐츠를 작성한 사용자 userId
 * @param sourceType    콘텐츠 유형 (POST / COMMENT)
 * @param sourceId      위기가 감지된 게시글 또는 댓글 ID
 * @param isRead        읽음 여부
 * @param createdAt     알림 생성 시각
 */
@Schema(description = "가디언 위기 알림 응답")
public record GuardianAlertResponse(

        @Schema(description = "알림 PK", example = "1")
        Long id,

        @Schema(description = "위기 콘텐츠 작성자 userId", example = "42")
        Long triggerUserId,

        @Schema(description = "콘텐츠 유형 (POST / COMMENT)", example = "POST")
        CrisisSourceType sourceType,

        @Schema(description = "위기가 감지된 게시글 또는 댓글 ID", example = "7")
        Long sourceId,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "알림 생성 시각", example = "2026-03-17T14:30:00")
        LocalDateTime createdAt

) {

    /**
     * GuardianAlert 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param alert 변환할 GuardianAlert 엔티티
     * @return GuardianAlertResponse 인스턴스
     */
    public static GuardianAlertResponse from(GuardianAlert alert) {
        return new GuardianAlertResponse(
                alert.getId(),
                alert.getTriggerUserId(),
                alert.getSourceType(),
                alert.getSourceId(),
                alert.isRead(),
                alert.getCreatedAt()
        );
    }
}
