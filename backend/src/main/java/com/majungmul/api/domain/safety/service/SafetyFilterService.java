package com.majungmul.api.domain.safety.service;

import com.majungmul.api.domain.safety.dto.SafetyCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 세이프티 필터 서비스.
 *
 * <p>게시글·댓글 작성 전 호출되어 콘텐츠를 두 단계로 심사한다:
 * <ol>
 *   <li><b>위기(CRISIS) 단계</b> — 자해·극단적 선택을 직접 암시하는 표현 감지.
 *       감지 시 게시 차단 + 가디언 알림 확장 포인트 트리거.
 *   </li>
 *   <li><b>차단(BLOCKED) 단계</b> — 타인을 향한 폭언·혐오 표현 등 커뮤니티 규칙 위반.
 *       감지 시 게시만 차단.
 *   </li>
 * </ol>
 *
 * <p>현재 구현: 키워드 기반 정적 필터 (Phase 2).
 * 향후 Phase 3에서 외부 AI API(예: Gemini Safety, OpenAI Moderation)로 교체 예정.
 * 교체 시 이 클래스의 {@code check()} 메서드 시그니처만 유지하면 호출 측 변경 없음.
 *
 * <p>⚠️ 익명성 보호: 필터 결과 로그에 원본 콘텐츠를 출력하지 않는다.
 * 감지된 키워드는 WARN 이하 로그로만 기록한다.
 */
@Slf4j
@Service
public class SafetyFilterService {

    // ─────────────────────────────────────────────────────────────────
    // 키워드 목록 — 운영 환경에서는 DB 또는 외부 설정으로 관리 권장
    // ─────────────────────────────────────────────────────────────────

    /**
     * 위기 수준 키워드: 자해·극단적 선택과 직접적으로 연관된 표현.
     *
     * <p>이 목록에 매칭되면 {@link SafetyCheckResult#crisis()}가 반환되어
     * 가디언 알림 확장 포인트를 트리거한다.
     *
     * <p>⚠️ 실제 서비스에서는 전문 심리 상담사·콘텐츠 모더레이션 팀이 관리해야 함.
     */
    private static final List<String> CRISIS_KEYWORDS = List.of(
            "죽고싶다", "죽고 싶다", "죽고싶어", "죽고 싶어",
            "사라지고싶다", "사라지고 싶다",
            "살기싫다", "살기 싫다", "살기싫어",
            "극단적선택", "극단적 선택",
            "스스로목숨", "스스로 목숨",
            "자살", "자해할거야", "자해할 거야"
    );

    /**
     * 차단 수준 키워드: 혐오·폭력·심각한 욕설 등 커뮤니티 가이드라인 위반 표현.
     *
     * <p>이 목록에만 매칭되면 게시 차단만 이루어지고, 가디언 알림은 발생하지 않는다.
     *
     * <p>⚠️ 실제 서비스에서는 전문 모더레이션 도구(예: Perspective API)로 대체 권장.
     */
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "패죽이", "죽여버", "죽여버리",
            "혐오한다", "쓸모없는놈", "쓸모 없는 놈",
            "꺼져라", "꺼져버려"
    );

    // ─────────────────────────────────────────────────────────────────
    // 공개 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 콘텐츠에 대한 세이프티 검사를 수행한다.
     *
     * <p>호출 흐름:
     * <pre>
     * PostService / CommentService
     *   → safetyFilterService.check(content)
     *   → SafetyCheckResult 반환
     *   → result.isCrisis() → 가디언 알림 확장 포인트 (주석 참조)
     *   → result.isBlocked() → BusinessException(POST_BLOCKED_BY_SAFETY)
     * </pre>
     *
     * @param content 검사할 텍스트 (게시글 제목+본문 또는 댓글 본문)
     * @return 검사 결과 — safe / blocked / crisis 중 하나
     */
    public SafetyCheckResult check(String content) {
        if (content == null || content.isBlank()) {
            return SafetyCheckResult.safe();
        }

        // 대소문자·공백 정규화 (한국어는 공백만 처리)
        String normalized = content.toLowerCase(Locale.KOREAN).replaceAll("\\s+", "");

        // ① 위기 키워드 먼저 검사 (우선순위 높음)
        List<String> crisisMatched = scanKeywords(normalized, CRISIS_KEYWORDS);
        if (!crisisMatched.isEmpty()) {
            log.warn("[Safety] 위기 수준 키워드 감지. matchCount={}", crisisMatched.size());
            return SafetyCheckResult.crisis(crisisMatched);
        }

        // ② 차단 키워드 검사
        List<String> blockedMatched = scanKeywords(normalized, BLOCKED_KEYWORDS);
        if (!blockedMatched.isEmpty()) {
            log.warn("[Safety] 차단 키워드 감지. matchCount={}", blockedMatched.size());
            return SafetyCheckResult.blocked(blockedMatched);
        }

        return SafetyCheckResult.safe();
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 유틸리티
    // ─────────────────────────────────────────────────────────────────

    /**
     * 정규화된 콘텐츠에서 키워드 목록과 매칭되는 항목을 수집한다.
     *
     * @param normalized 공백 제거·소문자 변환된 콘텐츠
     * @param keywords   검사할 키워드 목록
     * @return 감지된 키워드 목록 (없으면 빈 리스트)
     */
    private List<String> scanKeywords(String normalized, List<String> keywords) {
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            String normalizedKeyword = keyword.toLowerCase(Locale.KOREAN).replaceAll("\\s+", "");
            if (normalized.contains(normalizedKeyword)) {
                matched.add(keyword);
            }
        }
        return matched;
    }
}
