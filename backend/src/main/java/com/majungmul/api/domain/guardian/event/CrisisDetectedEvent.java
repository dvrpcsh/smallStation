package com.majungmul.api.domain.guardian.event;

import java.util.List;

/**
 * 세이프티 필터가 위기(CRISIS) 수준 키워드를 감지했을 때 발행되는 도메인 이벤트.
 *
 * <p>발행 흐름:
 * <pre>
 * PostService / CommentService
 *   → safetyFilterService.check(content) → crisis=true
 *   → ApplicationEventPublisher.publishEvent(CrisisDetectedEvent)
 *   → GuardianAlertListener (비동기 수신)
 * </pre>
 *
 * <p>이벤트 기반 방식을 선택한 이유:
 * <ul>
 *   <li>Post/Comment 도메인이 Guardian 도메인에 직접 의존하지 않아 결합도 감소.</li>
 *   <li>@Async 리스너로 가디언 알림 처리를 메인 트랜잭션과 분리 — 알림 실패가 게시 성공에 영향을 주지 않음.</li>
 * </ul>
 *
 * <p>⚠️ 익명성 보호:
 * <ul>
 *   <li>{@code matchedKeywords}는 내부 감사 로그 전용이며 API 응답에 절대 포함하지 않는다.</li>
 *   <li>콘텐츠 원문은 이벤트에 포함하지 않는다 — sourceId(PK)로 추적만 가능하게 설계.</li>
 * </ul>
 *
 * @param triggerUserId   위기 콘텐츠를 작성한 사용자 PK (익명 사용자 포함)
 * @param sourceId        위기가 감지된 게시글 또는 댓글 ID
 * @param sourceType      위기 발생 출처 (POST / COMMENT)
 * @param matchedKeywords 감지된 위기 키워드 목록 (감사 로그 전용)
 */
public record CrisisDetectedEvent(
        Long triggerUserId,
        Long sourceId,
        CrisisSourceType sourceType,
        List<String> matchedKeywords
) {}
