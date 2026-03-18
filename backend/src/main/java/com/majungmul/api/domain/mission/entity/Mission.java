package com.majungmul.api.domain.mission.entity;

import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 미션(Mission) 엔티티.
 *
 * <p>간이역 서비스의 '행동 활성화(BA) 엔진' 핵심 도메인.
 * 고립 청년이 '이불 정리', '물 한 잔 마시기' 같은 소소한 성취를 쌓을 수 있도록
 * 미션을 정의한다. 완료 시 {@code rewardAmount} 만큼 포인트가 적립된다.
 *
 * <p>DB 테이블: {@code missions}
 *
 * <p>주요 컬럼:
 * <ul>
 *   <li>{@code title}         — 미션 제목 (예: "오늘 이불 정리하기")</li>
 *   <li>{@code description}   — 미션 상세 설명</li>
 *   <li>{@code reward_amount} — 완료 시 지급할 포인트</li>
 *   <li>{@code category}      — LIFE / EMOTION / RELATION</li>
 *   <li>{@code is_active}     — 노출 여부 (false 이면 목록에서 제외)</li>
 * </ul>
 */
@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 미션 제목.
     * 클라이언트 목록 화면에 노출되는 주요 텍스트.
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 미션 상세 설명.
     * 유저가 어떻게 수행해야 하는지 안내하는 문장.
     */
    @Column(length = 500)
    private String description;

    /**
     * 완료 시 지급할 포인트.
     * {@code UserService.addPoint(userId, rewardAmount)} 호출 시 사용된다.
     */
    @Column(name = "reward_amount", nullable = false)
    private int rewardAmount;

    /**
     * 미션 카테고리 — LIFE / EMOTION / RELATION.
     * 클라이언트 필터링 및 UX 분류에 활용.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionCategory category;

    /**
     * 미션 활성화 여부.
     * false 이면 {@code GET /api/v1/missions} 목록에서 제외된다.
     * 비활성화된 미션은 완료 처리도 불가능하다.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 미션을 생성한다.
     *
     * @param title        미션 제목
     * @param description  미션 설명
     * @param rewardAmount 완료 시 지급 포인트 (양수)
     * @param category     미션 카테고리
     * @return 활성화 상태의 새 Mission 인스턴스
     */
    public static Mission create(String title, String description, int rewardAmount, MissionCategory category) {
        Mission mission = new Mission();
        mission.title = title;
        mission.description = description;
        mission.rewardAmount = rewardAmount;
        mission.category = category;
        mission.isActive = true;
        return mission;
    }

    // ─────────────────────────────────────────────────────────────────
    // 비즈니스 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 미션을 비활성화한다. 운영 중 특정 미션을 숨길 때 사용.
     */
    public void deactivate() {
        this.isActive = false;
    }
}
