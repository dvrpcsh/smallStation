package com.majungmul.api.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 작성 요청 DTO.
 *
 * <p>AI 세이프티 필터는 제목과 본문을 합산하여 검사한다.
 * 필터를 통과한 경우에만 게시글이 저장된다.
 *
 * @param title   게시글 제목 (1~100자, 필수)
 * @param content 게시글 본문 (1~5000자, 필수)
 */
public record PostCreateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 1, max = 100, message = "제목은 1~100자 사이여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(min = 1, max = 5000, message = "내용은 1~5000자 사이여야 합니다.")
        String content
) {}
