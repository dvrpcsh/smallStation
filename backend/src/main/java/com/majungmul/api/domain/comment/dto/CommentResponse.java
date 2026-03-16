package com.majungmul.api.domain.comment.dto;

import com.majungmul.api.domain.comment.entity.Comment;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO.
 *
 * <p>게시글 상세 페이지에서 댓글 목록 표시에 사용된다.
 *
 * <p>⚠️ 익명성: authorNickname이 null이면 "익명"으로 변환하여 반환.
 *
 * @param commentId       댓글 ID
 * @param content         댓글 본문
 * @param authorNickname  작성자 닉네임 (없으면 "익명")
 * @param isMyComment     요청자가 작성자인지 여부 (삭제 버튼 노출 여부 결정)
 * @param createdAt       작성 시각
 */
public record CommentResponse(
        Long commentId,
        String content,
        String authorNickname,
        boolean isMyComment,
        LocalDateTime createdAt
) {

    /**
     * Comment 엔티티와 요청자 ID로 응답 DTO를 생성한다.
     *
     * @param comment       조회된 댓글 엔티티 (author fetch join 필수)
     * @param requestUserId 현재 요청자의 userId — 본인 댓글 여부 판단에 사용
     * @return 댓글 응답 DTO
     */
    public static CommentResponse from(Comment comment, Long requestUserId) {
        String nickname = comment.getAuthor().getNickname() != null
                ? comment.getAuthor().getNickname()
                : "익명";

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                nickname,
                comment.isAuthor(requestUserId),
                comment.getCreatedAt()
        );
    }
}
