package com.majungmul.api.domain.comment.entity;

import com.majungmul.api.domain.post.entity.Post;
import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 게시글 댓글 엔티티.
 *
 * <p>고립 청년들이 서로의 게시글에 공감과 응원을 전하는 수단.
 * 댓글 역시 AI 세이프티 필터를 통과한 후에만 저장된다.
 *
 * <p>DB 테이블: {@code comments}
 *
 * <p>연관 관계:
 * <ul>
 *   <li>{@code post}   — 소속 게시글 (ManyToOne LAZY). 게시글 삭제 시 댓글도 논리 삭제.</li>
 *   <li>{@code author} — 댓글 작성자 (ManyToOne LAZY). 탈퇴 사용자 댓글도 보존.</li>
 * </ul>
 *
 * <p>⚠️ 익명성 보호: 댓글 작성자 닉네임이 null이면 "익명"으로 표시.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_post_id", columnList = "post_id"),
                @Index(name = "idx_comments_author_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용 기본 생성자 — 외부 직접 생성 방지
@ToString(exclude = {"post", "author", "content"})  // ⚠️ 익명성·보안: 개인 식별 가능 필드 로그 제외
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 댓글이 속한 게시글.
     *
     * <p>LAZY 로딩: 댓글 조회 시 게시글 정보가 불필요한 경우 불필요한 JOIN 방지.
     * 댓글 목록은 항상 특정 게시글 ID 기준으로 조회되므로 post 정보 재조회 불필요.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * 댓글 작성자.
     *
     * <p>LAZY 로딩: 댓글 목록에서 닉네임 표시가 필요한 경우 fetch join 사용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    /**
     * 댓글 본문. AI 세이프티 필터 검사 대상.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 논리 삭제 플래그.
     * true이면 목록 조회에서 제외. 물리 삭제 없이 데이터 보존.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 생성한다. AI 세이프티 필터 통과 후 이 메서드가 호출되어야 한다.
     *
     * @param post    댓글을 달 게시글
     * @param author  댓글 작성자
     * @param content 댓글 본문 (세이프티 필터 적용됨)
     * @return 저장 준비된 Comment 인스턴스
     */
    public static Comment create(Post post, User author, String content) {
        Comment comment = new Comment();
        comment.post = post;
        comment.author = author;
        comment.content = content;
        comment.isDeleted = false;
        return comment;
    }

    // ─────────────────────────────────────────────────────────────────
    // 비즈니스 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 댓글을 논리 삭제 처리한다.
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * 요청자가 이 댓글의 작성자인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 작성자이면 true
     */
    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }
}
