package com.majungmul.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 익명 로그인 요청 DTO.
 *
 * <p>클라이언트(앱)가 최초 실행 시 생성한 기기 고유 ID를 전송한다.
 * 이 ID를 기반으로 서버는 사용자를 식별하거나 신규 생성한다.
 *
 * <p>개인 정보를 전혀 요구하지 않는다 — 간이역 서비스의 핵심 철학.
 */
@Schema(description = "익명 로그인 요청 — 기기 고유 ID만으로 서비스 이용 가능")
public record AnonymousLoginRequest(

        @Schema(
            description = "클라이언트가 최초 실행 시 생성하는 기기 고유 식별자 (UUID 권장)",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank(message = "기기 ID는 필수입니다.")
        @Size(min = 10, max = 100, message = "기기 ID는 10자 이상 100자 이하여야 합니다.")
        String deviceId

) {}
