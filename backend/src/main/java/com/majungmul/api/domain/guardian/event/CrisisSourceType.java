package com.majungmul.api.domain.guardian.event;

/**
 * 위기 감지 이벤트가 발생한 콘텐츠 출처 구분.
 *
 * <p>가디언 알림에서 어떤 유형의 콘텐츠에서 위기 키워드가 발견됐는지 기록하여,
 * 가디언이 원문을 추적·확인할 수 있도록 한다.
 */
public enum CrisisSourceType {

    /** 게시글 작성 시 위기 감지 */
    POST,

    /** 댓글 작성 시 위기 감지 */
    COMMENT
}
