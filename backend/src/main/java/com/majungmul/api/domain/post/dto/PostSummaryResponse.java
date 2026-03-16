package com.majungmul.api.domain.post.dto;

import com.majungmul.api.domain.post.entity.Post;

import java.time.LocalDateTime;

/**
 * 게시글 목록(페이징) 응답 DTO.
 *
 * <p>목록에서는 본문(content)을 제외한 요약 정보만 반환하여 응답 크기를 최소화한다.
 * 댓글 수는 별도 카운트 쿼리로 제공되며, 목록 조회 최적화를 위해 캐싱 가능.
 *
 * <p>⚠️ 익명성: authorNickname이 null이면 "익명"으로 표시. 클라이언트 책임이 아닌
 * 이 DTO에서 변환하여 서버가 일관된 표현을 보장한다.
 *
 * @param postId          게시글 ID
 * @param title           게시글 제목
 * @param authorNickname  작성자 닉네임 (없으면 "익명")
 * @param commentCount    댓글 수
 * @param createdAt       작성 시각
 */
public record PostSummaryResponse(
        Long postId,
        String title,
        String authorNickname,
        long commentCount,
        LocalDateTime createdAt
) {

    /**
     * Post 엔티티와 댓글 수로 목록용 응답 DTO를 생성한다.
     *
     * @param post         조회된 게시글 엔티티 (author fetch join 필수)
     * @param commentCount 해당 게시글의 댓글 수
     * @return 목록용 요약 응답
     */
    public static PostSummaryResponse from(Post post, long commentCount) {
        // 닉네임 미설정 사용자는 "익명"으로 통일 — 익명성 우선 원칙
        String nickname = post.getAuthor().getNickname() != null
                ? post.getAuthor().getNickname()
                : "익명";

        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                nickname,
                commentCount,
                post.getCreatedAt()
        );
    }
}
