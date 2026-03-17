package com.majungmul.api.domain.post.service;

import com.majungmul.api.domain.comment.repository.CommentRepository;
import com.majungmul.api.domain.guardian.event.CrisisDetectedEvent;
import com.majungmul.api.domain.guardian.event.CrisisSourceType;
import com.majungmul.api.domain.post.dto.PostCreateRequest;
import com.majungmul.api.domain.post.dto.PostResponse;
import com.majungmul.api.domain.post.dto.PostSummaryResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글(Post) 비즈니스 로직 서비스.
 *
 * <p>담당 기능:
 * <ol>
 *   <li>게시글 페이징 목록 조회</li>
 *   <li>게시글 상세 조회</li>
 *   <li>게시글 작성 (AI 세이프티 필터 → 저장)</li>
 *   <li>게시글 삭제 (작성자 본인 확인 → 논리 삭제)</li>
 * </ol>
 *
 * <p>데이터 흐름:
 * <pre>
 * PostController
 *   → PostService (비즈니스 로직, 트랜잭션 경계)
 *     → SafetyFilterService (콘텐츠 검사)
 *     → PostRepository      (JPA CRUD)
 *     → UserRepository      (작성자 조회)
 *     → CommentRepository   (댓글 수 집계)
 * </pre>
 *
 * <p>⚠️ 익명성 보호: 로그에 게시글 제목·내용을 직접 출력하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final SafetyFilterService safetyFilterService;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────
    // 게시글 목록 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글 목록을 최신순으로 페이징 조회한다.
     *
     * <p>목록에는 본문을 제외한 요약 정보(제목, 작성자 닉네임, 댓글 수)만 포함된다.
     * 각 게시글의 댓글 수는 개별 카운트 쿼리로 집계된다.
     * (게시글 수가 많아지면 별도 캐싱 레이어 도입 권장)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 게시글 수 (기본값 10)
     * @return 페이징된 게시글 요약 목록
     */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // fetch join으로 author를 함께 조회하여 N+1 방지
        Page<Post> posts = postRepository.findAllActiveWithAuthor(pageable);

        return posts.map(post -> {
            long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
            return PostSummaryResponse.from(post, commentCount);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // 게시글 상세 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글 상세 정보를 조회한다.
     *
     * @param postId        조회할 게시글 ID
     * @param requestUserId 요청자 userId (본인 게시글 여부 판단에 사용)
     * @return 게시글 상세 응답
     * @throws BusinessException POST_NOT_FOUND — 존재하지 않거나 삭제된 게시글
     */
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long requestUserId) {
        Post post = postRepository.findActiveByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return PostResponse.from(post, requestUserId);
    }

    // ─────────────────────────────────────────────────────────────────
    // 게시글 작성
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 작성한다.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. 작성자 조회 (JWT userId → User 엔티티)
     * 2. AI 세이프티 필터 — 제목 + 본문 합산 검사
     *    ├ crisis 감지  → 가디언 알림 확장 포인트 → POST_BLOCKED_BY_SAFETY 예외
     *    └ blocked 감지 → POST_BLOCKED_BY_SAFETY 예외
     * 3. Post 엔티티 생성 및 저장
     * </pre>
     *
     * @param request 게시글 제목·본문
     * @param userId  JWT에서 추출된 작성자 ID
     * @return 저장된 게시글 상세 응답
     * @throws BusinessException USER_NOT_FOUND         — 탈퇴한 사용자
     * @throws BusinessException POST_BLOCKED_BY_SAFETY — 세이프티 필터 차단
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {

        // ① 작성자 조회
        User author = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ② AI 세이프티 필터 — 제목과 본문을 합산하여 검사
        String combinedContent = request.title() + " " + request.content();
        SafetyCheckResult safetyResult = safetyFilterService.check(combinedContent);

        // ③ 게시글 저장 (위기 감지 여부와 무관하게 차단 처리)
        if (safetyResult.blocked()) {
            if (safetyResult.crisis()) {
                // 게시는 차단하되, 위기 이벤트를 발행하여 가디언 알림 트리거
                // sourceId를 -1로 설정: 게시글이 저장되지 않아 ID가 없는 상태
                // Phase 4에서 임시 저장 또는 별도 위기 기록 테이블 도입 검토
                eventPublisher.publishEvent(
                        new CrisisDetectedEvent(userId, -1L, CrisisSourceType.POST,
                                safetyResult.matchedKeywords()));
                log.warn("[Safety] 위기 수준 게시글 차단 및 가디언 알림 발행. userId={}", userId);
            }
            throw new BusinessException(ErrorCode.POST_BLOCKED_BY_SAFETY);
        }

        Post post = Post.create(author, request.title(), request.content());
        Post saved = postRepository.save(post);

        if (safetyResult.crisis()) {
            // 위기 감지되었으나 차단 수준은 아닌 경우 (현재 SafetyCheckResult 설계상 미발생,
            // 향후 정책 변경 대비 방어 코드로 유지)
            eventPublisher.publishEvent(
                    new CrisisDetectedEvent(userId, saved.getId(), CrisisSourceType.POST,
                            safetyResult.matchedKeywords()));
            log.warn("[Safety] 위기 수준 게시글 감지 및 가디언 알림 발행. userId={}, postId={}", userId, saved.getId());
        }
        log.info("[Post] 게시글 작성 완료. postId={}", saved.getId());

        return PostResponse.from(saved, userId);
    }

    // ─────────────────────────────────────────────────────────────────
    // 게시글 삭제
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 논리 삭제한다.
     *
     * <p>본인 게시글만 삭제 가능하다. 작성자가 아닌 사용자의 요청은 FORBIDDEN 예외를 발생시킨다.
     *
     * @param postId  삭제할 게시글 ID
     * @param userId  요청자 ID
     * @throws BusinessException POST_NOT_FOUND — 존재하지 않거나 이미 삭제된 게시글
     * @throws BusinessException FORBIDDEN      — 본인 게시글이 아닌 경우
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findActiveByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 본인 게시글 여부 확인 — 다른 사람의 게시글 삭제 시도 방어
        if (!post.isAuthor(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        post.delete();
        log.info("[Post] 게시글 삭제 완료. postId={}", postId);
    }
}
