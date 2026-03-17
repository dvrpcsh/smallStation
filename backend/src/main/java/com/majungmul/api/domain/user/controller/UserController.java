package com.majungmul.api.domain.user.controller;

import com.majungmul.api.domain.user.dto.UserResponse;
import com.majungmul.api.domain.user.dto.UserUpdateRequest;
import com.majungmul.api.domain.user.service.UserService;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자(User) API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>GET   /api/v1/users/me          — 현재 로그인 사용자 프로필 조회</li>
 *   <li>PATCH /api/v1/users/me/nickname — 닉네임 수정 (ANONYMOUS → USER 역할 승격 포함)</li>
 * </ul>
 *
 * <p>모든 엔드포인트는 JWT 인증이 필요하다.
 * {@code @AuthenticationPrincipal Long userId}로 SecurityContext에서 userId를 직접 수신한다.
 *
 * <p>모든 응답은 {@link ApiResponse} 규격을 따른다.
 */
@Tag(name = "User", description = "사용자 프로필 API — 닉네임 수정 및 포인트 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/users/me
    // ─────────────────────────────────────────────────────────────────

    /**
     * 현재 로그인한 사용자의 프로필을 조회한다.
     *
     * <p>간이역 서비스에서 사용자는 자신의 닉네임과 포인트를 확인하여
     * 미션 참여 동기를 이어나갈 수 있다.
     */
    @Operation(
        summary = "내 프로필 조회",
        description = """
            현재 로그인한 사용자의 프로필(id, 닉네임, 포인트, 역할)을 반환합니다.
            - 닉네임 미설정 시 null 반환 (클라이언트에서 '익명'으로 표시)
            - 포인트는 미션 완료 시 적립되며 상품권 교환에 사용됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 사용자 (U001)")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        UserResponse result = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /api/v1/users/me/nickname
    // ─────────────────────────────────────────────────────────────────

    /**
     * 닉네임을 수정한다.
     *
     * <p>ANONYMOUS 사용자가 처음 닉네임을 등록하면 역할이 USER로 승격된다.
     * 고립 청년이 커뮤니티에 자신의 이름을 올리는 첫 번째 소속감 경험.
     */
    @Operation(
        summary = "닉네임 수정",
        description = """
            닉네임을 수정합니다.
            - ANONYMOUS 사용자가 처음 등록하면 역할이 USER로 자동 승격됩니다.
            - 이미 사용 중인 닉네임은 사용할 수 없습니다. (U003)
            - 닉네임은 1자 이상 20자 이하여야 합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 사용자 (U001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "닉네임 중복 (U003)")
    })
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(
            @Valid @RequestBody UserUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        UserResponse result = userService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success("닉네임이 수정되었습니다.", result));
    }
}
