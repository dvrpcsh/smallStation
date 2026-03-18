package com.majungmul.api.domain.mission.entity;

/**
 * 미션 카테고리 분류.
 *
 * <p>고립 청년의 회복 단계에 따라 미션을 분류한다.
 * 클라이언트에서 카테고리 필터링 및 UX 표현에 활용된다.
 *
 * <ul>
 *   <li>LIFE    — 생활습관 미션 (이불 정리, 물 한 잔 마시기 등 일상 회복)</li>
 *   <li>EMOTION — 정서 미션 (오늘 기분 기록, 감사 일기 등 정서 안정)</li>
 *   <li>RELATION — 관계 미션 (커뮤니티 댓글 달기, 가족에게 안부 문자 등 연결 회복)</li>
 * </ul>
 */
public enum MissionCategory {

    /** 생활습관 회복 — 기본적인 일상 루틴을 되찾는 미션 */
    LIFE,

    /** 정서 안정 — 자신의 감정을 인식하고 표현하는 미션 */
    EMOTION,

    /** 관계 회복 — 타인과의 작은 연결을 시도하는 미션 */
    RELATION
}
