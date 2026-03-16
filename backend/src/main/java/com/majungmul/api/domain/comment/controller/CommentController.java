package com.majungmul.api.domain.comment.controller;

import com.majungmul.api.domain.comment.dto.CommentCreateRequest;
import com.majungmul.api.domain.comment.dto.CommentResponse;
import com.majungmul.api.domain.comment.service.CommentService;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글(Comment) API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>GET    /api/v1/posts/{postId}/comments              — 댓글 목록 조회</li>
 *   <li>POST   /api/v1/posts/{postId}/comments              — 댓글 작성 (AI 세이프티 필터 적용)</li>
 *   <li>DELETE /api/v1/posts/{postId}/comments/{commentId}  — 댓글 삭제 (본인만 가능)</li>
 * </ul>
 *
 * <p>모든 엔드포인트는 JWT 인증이 필요하다.
 * 모든 응답은 {@link ApiResponse} 규격을 따른다.
 */
@Tag(name = "Comment", description = "댓글 API — AI 세이프티 필터 적용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/posts/{postId}/comments
    // ─────────────────────────────────────────────────────────────────

    /**
     * 특정 게시글의 댓글 목록을 조회한다.
     *
     * <p>작성 시각 오름차순 — 대화의 흐름대로 읽을 수 있도록.
     */
    @Operation(
        summary = "댓글 목록 조회",
        description = """
            특정 게시글의 댓글 목록을 작성 순으로 반환합니다.
            - 삭제된 댓글은 제외됩니다.
            - 작성자 닉네임 미설정 시 '익명'으로 표시됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 (P001)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        List<CommentResponse> result = commentService.getComments(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/posts/{postId}/comments
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 작성한다.
     *
     * <p>공감과 응원 한 마디가 고립 청년에게 큰 힘이 될 수 있다.
     * AI 세이프티 필터를 통과하지 못하면 댓글 등록이 차단된다 (P002).
     */
    @Operation(
        summary = "댓글 작성",
        description = """
            댓글을 작성합니다. AI 세이프티 필터가 자동 적용됩니다.
            - 차단 키워드 감지 시: 400 응답 + P002 에러 코드
            - 위기 수준 키워드 감지 시: 동일하게 차단 + 가디언 알림 (Phase 3)
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 세이프티 필터 차단 (P002)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 (P001)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        CommentResponse result = commentService.createComment(postId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 등록되었습니다.", result));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/v1/posts/{postId}/comments/{commentId}
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 삭제한다 (논리 삭제).
     *
     * <p>본인 댓글만 삭제 가능. 다른 사람의 댓글 삭제 시도 시 403 응답.
     */
    @Operation(
        summary = "댓글 삭제",
        description = "본인 댓글을 논리 삭제합니다. 다른 사람의 댓글은 삭제할 수 없습니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 댓글 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음 (P003)")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }
}
