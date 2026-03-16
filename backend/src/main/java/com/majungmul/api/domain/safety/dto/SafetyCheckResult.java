package com.majungmul.api.domain.safety.dto;

import java.util.List;

/**
 * AI 세이프티 필터 검사 결과.
 *
 * <p>검사 결과는 두 가지 심각도를 구분한다:
 * <ul>
 *   <li>{@code blocked}  — 콘텐츠를 게시 차단해야 하는 경우. true이면 API에서 P002 에러 반환.</li>
 *   <li>{@code crisis}   — 위기(자해·극단적 선택 암시) 수준의 키워드 감지. true이면 가디언 알림 트리거.</li>
 * </ul>
 *
 * <p>우선순위: crisis → blocked → 정상
 *
 * @param blocked         게시 차단 여부 (crisis=true이면 항상 blocked=true)
 * @param crisis          위기 수준 키워드 포함 여부 (가디언 알림 대상)
 * @param matchedKeywords 감지된 키워드 목록 (내부 감사 로그 전용 — 사용자에게 노출 금지)
 */
public record SafetyCheckResult(
        boolean blocked,
        boolean crisis,
        List<String> matchedKeywords
) {

    /** 안전한 콘텐츠임을 나타내는 정적 팩토리. */
    public static SafetyCheckResult safe() {
        return new SafetyCheckResult(false, false, List.of());
    }

    /** 일반 차단(위기 아님) 결과를 생성하는 정적 팩토리. */
    public static SafetyCheckResult blocked(List<String> matched) {
        return new SafetyCheckResult(true, false, matched);
    }

    /** 위기 감지 결과를 생성하는 정적 팩토리. 위기는 항상 차단도 포함한다. */
    public static SafetyCheckResult crisis(List<String> matched) {
        return new SafetyCheckResult(true, true, matched);
    }
}
