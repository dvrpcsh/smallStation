package com.majungmul.api.domain.guardian.controller;

import com.majungmul.api.domain.guardian.dto.GuardianAlertResponse;
import com.majungmul.api.domain.guardian.entity.GuardianAlert;
import com.majungmul.api.domain.guardian.repository.GuardianAlertRepository;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 가디언 전용 API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>GET /api/v1/guardian/alerts — 위기 알림 목록 조회 (읽음 처리 포함)</li>
 * </ul>
 *
 * <p>GUARDIAN 역할만 접근 가능하다 (SecurityConfig에서 경로 보호).
 * ANONYMOUS·USER 역할로 접근 시 403 응답.
 */
@Tag(name = "Guardian", description = "가디언 전용 API — 위기 알림 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianAlertController {

    private final GuardianAlertRepository guardianAlertRepository;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/guardian/alerts
    // ─────────────────────────────────────────────────────────────────

    /**
     * 현재 로그인한 가디언의 위기 알림 목록을 조회한다.
     *
     * <p>조회와 동시에 미읽음 알림을 모두 읽음 처리(markAsRead)한다.
     * 고립 청년들의 위기 신호를 가디언이 놓치지 않도록 즉각 확인 유도.
     */
    @Operation(
        summary = "위기 알림 목록 조회",
        description = """
            가디언에게 발송된 위기 알림 목록을 최신순으로 반환합니다.
            - 조회 시 미읽음 알림이 자동으로 읽음 처리됩니다.
            - GUARDIAN 역할만 접근 가능합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "가디언 권한 없음")
    })
    @Transactional
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<GuardianAlertResponse>>> getAlerts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        List<GuardianAlert> alerts =
                guardianAlertRepository.findAllByGuardianIdOrderByCreatedAtDesc(userId);

        // 미읽음 알림 일괄 읽음 처리 (Dirty Checking 활용)
        alerts.stream()
                .filter(alert -> !alert.isRead())
                .forEach(GuardianAlert::markAsRead);

        List<GuardianAlertResponse> result = alerts.stream()
                .map(GuardianAlertResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
