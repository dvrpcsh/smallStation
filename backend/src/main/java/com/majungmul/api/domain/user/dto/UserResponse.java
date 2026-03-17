package com.majungmul.api.domain.user.dto;

import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 프로필 응답 DTO.
 *
 * <p>⚠️ 익명성 보호: deviceId는 절대 포함하지 않는다.
 * 클라이언트에 노출되는 식별 정보는 id(PK)·nickname(선택)·point·role만 허용한다.
 *
 * @param id       사용자 PK
 * @param nickname 닉네임 (미등록 시 null — 커뮤니티에서 '익명' 표시)
 * @param point    보유 포인트 (미션 완료 시 적립)
 * @param role     현재 역할 (ANONYMOUS / USER / GUARDIAN / ADMIN)
 */
@Schema(description = "사용자 프로필 응답")
public record UserResponse(

        @Schema(description = "사용자 PK", example = "1")
        Long id,

        @Schema(description = "닉네임 (미등록 시 null)", example = "간이역청년")
        String nickname,

        @Schema(description = "보유 포인트", example = "150")
        int point,

        @Schema(description = "사용자 역할", example = "USER")
        UserRole role

) {

    /**
     * User 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param user 변환할 User 엔티티
     * @return UserResponse 인스턴스
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getPoint(),
                user.getRole()
        );
    }
}
