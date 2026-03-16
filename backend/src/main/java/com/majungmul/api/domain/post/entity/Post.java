package com.majungmul.api.domain.post.entity;

import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 익명 게시판 게시글 엔티티.
 *
 * <p>간이역 커뮤니티의 핵심 콘텐츠 단위. 고립 청년들이 자신의 감정과 일상을
 * 익명으로 나누는 공간이다. 게시글은 AI 세이프티 필터를 통과한 후에만 저장된다.
 *
 * <p>DB 테이블: {@code posts}
 *
 * <p>주요 컬럼:
 * <ul>
 *   <li>{@code author}       — 작성자 (User FK, 지연 로딩). 탈퇴 사용자도 기록 보존.</li>
 *   <li>{@code title}        — 게시글 제목 (최대 100자).</li>
 *   <li>{@code content}      — 게시글 본문 (TEXT 타입, 제한 없음).</li>
 *   <li>{@code is_deleted}   — 논리 삭제 플래그. true이면 조회에서 제외.</li>
 * </ul>
 *
 * <p>⚠️ 익명성 보호: 게시글 조회 시 작성자 닉네임이 null이면 "익명"으로 표시.
 * 로그에 게시글 본문 내용을 직접 출력하지 않는다.
 */
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_author_id", columnList = "user_id"),
                @Index(name = "idx_posts_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용 기본 생성자 — 외부 직접 생성 방지
@ToString(exclude = {"author", "title", "content"}) // ⚠️ 익명성·보안: 개인 식별 가능 필드 로그 제외
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 게시글 작성자.
     *
     * <p>LAZY 로딩: 게시글 목록 조회 시 작성자 정보가 불필요한 경우
     * 불필요한 JOIN을 방지하기 위해 지연 로딩 적용.
     * 닉네임이 필요한 경우 fetch join 또는 DTO Projection 사용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    /**
     * 게시글 제목. AI 세이프티 필터 검사 대상에 포함됨.
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 게시글 본문. AI 세이프티 필터 검사 대상에 포함됨.
     * TEXT 타입으로 길이 제한 없이 저장.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 논리 삭제 플래그.
     * true이면 목록·상세 조회에서 제외. 물리 삭제 없이 데이터 보존.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 생성한다. AI 세이프티 필터 통과 후 이 메서드가 호출되어야 한다.
     *
     * @param author  게시글 작성자 (JWT에서 추출된 userId로 조회된 User)
     * @param title   제목 (1~100자, 세이프티 필터 적용됨)
     * @param content 본문 (세이프티 필터 적용됨)
     * @return 저장 준비된 Post 인스턴스
     */
    public static Post create(User author, String title, String content) {
        Post post = new Post();
        post.author = author;
        post.title = title;
        post.content = content;
        post.isDeleted = false;
        return post;
    }

    // ─────────────────────────────────────────────────────────────────
    // 비즈니스 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 게시글을 논리 삭제 처리한다.
     *
     * <p>물리 삭제 대신 is_deleted 플래그 사용 이유:
     * 해당 게시글에 달린 댓글 데이터의 무결성 보존 및 감사 추적 목적.
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * 요청자가 이 게시글의 작성자인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 작성자이면 true
     */
    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }
}
