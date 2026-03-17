package com.majungmul.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 수정 요청 DTO.
 *
 * <p>ANONYMOUS 사용자가 닉네임을 등록하면 역할이 USER로 승격된다.
 * 이미 USER인 경우에는 닉네임만 변경된다.
 *
 * @param nickname 변경할 닉네임 (1~20자, 공백 불가)
 */
@Schema(description = "닉네임 수정 요청")
public record UserUpdateRequest(

        @Schema(description = "변경할 닉네임 (1~20자)", example = "간이역청년")
        @NotBlank(message = "닉네임은 공백일 수 없습니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
        String nickname

) {}
