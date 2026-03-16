package com.majungmul.api.domain.post.controller;

import com.majungmul.api.domain.post.dto.PostCreateRequest;
import com.majungmul.api.domain.post.dto.PostResponse;
import com.majungmul.api.domain.post.dto.PostSummaryResponse;
import com.majungmul.api.domain.post.service.PostService;
import com.majungmul.api.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글(Post) API 컨트롤러.
 *
 * <p>제공 API:
 * <ul>
 *   <li>GET  /api/v1/posts           — 게시글 목록 페이징 조회</li>
 *   <li>GET  /api/v1/posts/{postId}  — 게시글 상세 조회</li>
 *   <li>POST /api/v1/posts           — 게시글 작성 (AI 세이프티 필터 적용)</li>
 *   <li>DELETE /api/v1/posts/{postId} — 게시글 삭제 (본인만 가능)</li>
 * </ul>
 *
 * <p>모든 엔드포인트는 JWT 인증이 필요하다.
 * (익명 사용자도 {@code POST /api/v1/auth/anonymous}로 토큰 발급 후 이용 가능)
 *
 * <p>모든 응답은 {@link ApiResponse} 규격을 따른다.
 */
@Tag(name = "Post", description = "익명 게시판 API — AI 세이프티 필터 적용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/posts
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글 목록을 최신순으로 페이징 조회한다.
     *
     * <p>간이역 커뮤니티에서 다른 청년들의 이야기를 발견하는 첫 진입점.
     * 목록에는 본문 미포함 — 데이터 전송량 최소화.
     */
    @Operation(
        summary = "게시글 목록 조회",
        description = """
            게시글 목록을 최신순으로 페이징 조회합니다.
            - 삭제된 게시글은 제외됩니다.
            - 작성자 닉네임 미설정 시 '익명'으로 표시됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 게시글 수", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Page<PostSummaryResponse> result = postService.getPosts(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/posts/{postId}
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글 상세 정보를 조회한다.
     *
     * <p>댓글 목록은 {@code GET /api/v1/posts/{postId}/comments}로 별도 조회.
     */
    @Operation(
        summary = "게시글 상세 조회",
        description = """
            게시글 상세 정보를 반환합니다.
            - isMyPost=true이면 클라이언트에서 삭제 버튼을 노출합니다.
            - 댓글 목록은 GET /api/v1/posts/{postId}/comments 로 별도 조회합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 (P001)")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        PostResponse result = postService.getPost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/posts
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 작성한다.
     *
     * <p>고립 청년들이 자신의 감정과 일상을 자유롭게 나눌 수 있는 공간.
     * AI 세이프티 필터를 통과하지 못하면 게시가 차단된다 (P002).
     * 위기 수준 키워드 감지 시 가디언 알림 확장 포인트가 트리거된다.
     */
    @Operation(
        summary = "게시글 작성",
        description = """
            게시글을 작성합니다. AI 세이프티 필터가 자동 적용됩니다.
            - 차단 키워드 감지 시: 400 응답 + P002 에러 코드
            - 위기 수준 키워드 감지 시: 동일하게 차단 + 가디언 알림 (Phase 3)
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 세이프티 필터 차단 (P002)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        PostResponse result = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", result));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/v1/posts/{postId}
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 삭제한다 (논리 삭제).
     *
     * <p>본인 게시글만 삭제 가능. 다른 사람의 게시글 삭제 시도 시 403 응답.
     */
    @Operation(
        summary = "게시글 삭제",
        description = "본인 게시글을 논리 삭제합니다. 다른 사람의 게시글은 삭제할 수 없습니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 게시글 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 (P001)")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }
}
