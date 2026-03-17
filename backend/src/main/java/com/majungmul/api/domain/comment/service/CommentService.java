package com.majungmul.api.domain.comment.service;

import com.majungmul.api.domain.comment.dto.CommentCreateRequest;
import com.majungmul.api.domain.comment.dto.CommentResponse;
import com.majungmul.api.domain.comment.entity.Comment;
import com.majungmul.api.domain.comment.repository.CommentRepository;
import com.majungmul.api.domain.guardian.event.CrisisDetectedEvent;
import com.majungmul.api.domain.guardian.event.CrisisSourceType;
import com.majungmul.api.domain.post.entity.Post;
import com.majungmul.api.domain.post.repository.PostRepository;
import com.majungmul.api.domain.safety.dto.SafetyCheckResult;
import com.majungmul.api.domain.safety.service.SafetyFilterService;
import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.repository.UserRepository;
import com.majungmul.api.global.common.exception.BusinessException;
import com.majungmul.api.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글(Comment) 비즈니스 로직 서비스.
 *
 * <p>담당 기능:
 * <ol>
 *   <li>댓글 목록 조회 (특정 게시글 기준)</li>
 *   <li>댓글 작성 (AI 세이프티 필터 → 저장)</li>
 *   <li>댓글 삭제 (작성자 본인 확인 → 논리 삭제)</li>
 * </ol>
 *
 * <p>데이터 흐름:
 * <pre>
 * CommentController
 *   → CommentService (비즈니스 로직, 트랜잭션 경계)
 *     → SafetyFilterService (콘텐츠 검사)
 *     → CommentRepository   (JPA CRUD)
 *     → PostRepository      (게시글 유효성 확인)
 *     → UserRepository      (작성자 조회)
 * </pre>
 *
 * <p>⚠️ 익명성 보호: 로그에 댓글 내용을 직접 출력하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SafetyFilterService safetyFilterService;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────
    // 댓글 목록 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 특정 게시글의 댓글 목록을 작성 순(오름차순)으로 조회한다.
     *
     * @param postId        댓글을 조회할 게시글 ID
     * @param requestUserId 요청자 userId (본인 댓글 여부 판단)
     * @return 댓글 목록 (삭제된 댓글 제외, 작성 시간 오름차순)
     * @throws BusinessException POST_NOT_FOUND — 존재하지 않거나 삭제된 게시글
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, Long requestUserId) {
        // 게시글 유효성 확인 — 삭제된 게시글의 댓글 조회 방지
        if (!postRepository.existsByIdAndIsDeletedFalse(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        List<Comment> comments = commentRepository.findActiveByPostIdWithAuthor(postId);
        return comments.stream()
                .map(c -> CommentResponse.from(c, requestUserId))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // 댓글 작성
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 작성한다.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. 게시글 유효성 확인 (삭제 여부)
     * 2. 작성자 조회
     * 3. AI 세이프티 필터 — 댓글 본문 검사
     *    ├ crisis 감지  → 가디언 알림 확장 포인트 → POST_BLOCKED_BY_SAFETY 예외
     *    └ blocked 감지 → POST_BLOCKED_BY_SAFETY 예외
     * 4. Comment 엔티티 생성 및 저장
     * </pre>
     *
     * @param postId  댓글을 달 게시글 ID
     * @param request 댓글 본문
     * @param userId  JWT에서 추출된 작성자 ID
     * @return 저장된 댓글 응답
     * @throws BusinessException POST_NOT_FOUND         — 존재하지 않거나 삭제된 게시글
     * @throws BusinessException USER_NOT_FOUND         — 탈퇴한 사용자
     * @throws BusinessException POST_BLOCKED_BY_SAFETY — 세이프티 필터 차단
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {

        // ① 게시글 유효성 확인 — author fetch join 필요 없으므로 exists 쿼리 사용
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // ② 작성자 조회
        User author = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ③ AI 세이프티 필터
        SafetyCheckResult safetyResult = safetyFilterService.check(request.content());

        // ④ 댓글 저장 (위기 감지 시 차단 + 가디언 알림 발행)
        if (safetyResult.blocked()) {
            if (safetyResult.crisis()) {
                eventPublisher.publishEvent(
                        new CrisisDetectedEvent(userId, postId, CrisisSourceType.COMMENT,
                                safetyResult.matchedKeywords()));
                log.warn("[Safety] 위기 수준 댓글 차단 및 가디언 알림 발행. userId={}, postId={}", userId, postId);
            }
            throw new BusinessException(ErrorCode.POST_BLOCKED_BY_SAFETY);
        }

        Comment comment = Comment.create(post, author, request.content());
        Comment saved = commentRepository.save(comment);
        log.info("[Comment] 댓글 작성 완료. commentId={}, postId={}", saved.getId(), postId);

        return CommentResponse.from(saved, userId);
    }

    // ─────────────────────────────────────────────────────────────────
    // 댓글 삭제
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 논리 삭제한다.
     *
     * <p>본인 댓글만 삭제 가능하다.
     *
     * @param commentId 삭제할 댓글 ID
     * @param userId    요청자 ID
     * @throws BusinessException COMMENT_NOT_FOUND — 존재하지 않거나 이미 삭제된 댓글
     * @throws BusinessException FORBIDDEN          — 본인 댓글이 아닌 경우
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findActiveByIdWithAuthor(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        comment.delete();
        log.info("[Comment] 댓글 삭제 완료. commentId={}", commentId);
    }
}
