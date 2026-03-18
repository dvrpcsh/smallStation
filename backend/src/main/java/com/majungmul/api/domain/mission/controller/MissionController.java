package com.majungmul.api.domain.mission.controller;

import com.majungmul.api.domain.mission.dto.MissionCompleteResponse;
import com.majungmul.api.domain.mission.dto.MissionResponse;
import com.majungmul.api.domain.mission.service.MissionService;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 미션(Mission) API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>GET  /api/v1/missions                         — 활성 미션 목록 조회</li>
 *   <li>POST /api/v1/missions/{missionId}/complete     — 미션 완료 처리 및 포인트 보상 수령</li>
 * </ul>
 *
 * <p>모든 엔드포인트는 JWT 인증이 필요하다.
 * {@code @AuthenticationPrincipal Long userId}로 SecurityContext에서 userId를 직접 수신한다.
 *
 * <p>모든 응답은 {@link ApiResponse} 규격을 따른다.
 */
@Tag(name = "Mission", description = "행동 활성화(BA) 엔진 API — 미션 조회 및 완료 처리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/missions
    // ─────────────────────────────────────────────────────────────────

    /**
     * 활성화된 미션 목록을 조회한다.
     *
     * <p>고립 청년이 오늘 수행할 수 있는 미션 목록을 제공한다.
     * '이불 정리', '물 한 잔 마시기' 같은 소소한 성취로 하루를 시작할 수 있도록 돕는다.
     */
    @Operation(
        summary = "활성 미션 목록 조회",
        description = """
            현재 활성화된 미션 목록을 반환합니다.
            - is_active=true 인 미션만 포함됩니다.
            - 카테고리: LIFE(생활) / EMOTION(정서) / RELATION(관계)
            - 각 미션의 완료 시 지급 포인트(rewardAmount)를 확인할 수 있습니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MissionResponse>>> getActiveMissions() {
        List<MissionResponse> result = missionService.getActiveMissions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/missions/{missionId}/complete
    // ─────────────────────────────────────────────────────────────────

    /**
     * 미션을 완료 처리하고 포인트 보상을 수령한다.
     *
     * <p>유저가 미션을 완료하면 인증 로그가 기록되고 포인트가 적립된다.
     * 인증 로그 저장과 포인트 적립은 하나의 트랜잭션 내에서 원자적으로 처리된다.
     * 고립 청년이 작은 성취를 쌓아 회복의 실마리를 얻는 핵심 흐름.
     */
    @Operation(
        summary = "미션 완료 처리",
        description = """
            미션 완료를 처리하고 포인트를 지급합니다.
            - 동일한 미션은 하루에 한 번만 완료할 수 있습니다. (M003)
            - 완료 처리와 포인트 적립은 원자적으로 처리됩니다 (하나라도 실패 시 전체 취소).
            - 응답에 이번에 적립된 포인트(rewardedPoints)와 총 누적 포인트(totalPoints)가 포함됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미션 완료 및 포인트 적립 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 미션 (M001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "오늘 이미 완료한 미션 (M003)")
    })
    @PostMapping("/{missionId}/complete")
    public ResponseEntity<ApiResponse<MissionCompleteResponse>> completeMission(
            @Parameter(description = "완료할 미션 PK", example = "1") @PathVariable Long missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        MissionCompleteResponse result = missionService.completeMission(userId, missionId);
        return ResponseEntity.ok(ApiResponse.success("미션 완료! 포인트가 적립되었습니다.", result));
    }
}
