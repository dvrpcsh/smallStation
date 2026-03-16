package com.majungmul.api.domain.post.dto;

import com.majungmul.api.domain.post.entity.Post;

import java.time.LocalDateTime;

/**
 * 게시글 상세 조회 응답 DTO.
 *
 * <p>목록({@link PostSummaryResponse})과 달리 본문(content)을 포함한 전체 정보를 반환한다.
 * 댓글 목록은 별도 API({@code GET /api/v1/posts/{postId}/comments})로 조회한다.
 *
 * <p>⚠️ 익명성: authorNickname이 null이면 "익명"으로 변환하여 반환.
 *
 * @param postId          게시글 ID
 * @param title           게시글 제목
 * @param content         게시글 본문
 * @param authorNickname  작성자 닉네임 (없으면 "익명")
 * @param isMyPost        요청자가 작성자인지 여부 (삭제 버튼 노출 여부 결정)
 * @param createdAt       작성 시각
 * @param updatedAt       수정 시각
 */
public record PostResponse(
        Long postId,
        String title,
        String content,
        String authorNickname,
        boolean isMyPost,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * Post 엔티티와 요청자 ID로 상세 응답 DTO를 생성한다.
     *
     * @param post          조회된 게시글 엔티티 (author fetch join 필수)
     * @param requestUserId 현재 요청자의 userId — 본인 게시글 여부 판단에 사용
     * @return 상세 응답 DTO
     */
    public static PostResponse from(Post post, Long requestUserId) {
        String nickname = post.getAuthor().getNickname() != null
                ? post.getAuthor().getNickname()
                : "익명";

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                nickname,
                post.isAuthor(requestUserId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
