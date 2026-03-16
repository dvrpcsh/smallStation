package com.majungmul.api.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청 DTO.
 *
 * <p>게시글과 마찬가지로 AI 세이프티 필터 검사를 통과한 경우에만 저장된다.
 *
 * @param content 댓글 본문 (1~1000자, 필수)
 */
public record CommentCreateRequest(

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(min = 1, max = 1000, message = "댓글은 1~1000자 사이여야 합니다.")
        String content
) {}
