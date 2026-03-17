package com.majungmul.api.domain.guardian.entity;

import com.majungmul.api.domain.guardian.event.CrisisSourceType;
import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가디언 위기 알림 엔티티.
 *
 * <p>위기(CRISIS) 수준 콘텐츠 감지 시 생성되며,
 * 검증된 또래 상담사(GUARDIAN 역할 사용자)가 확인할 수 있도록 저장된다.
 *
 * <p>DB 테이블: {@code guardian_alerts}
 *
 * <p>주요 컬럼:
 * <ul>
 *   <li>{@code guardian_id}     — 알림을 수신할 가디언의 userId(PK)</li>
 *   <li>{@code trigger_user_id} — 위기 콘텐츠를 작성한 사용자 userId(PK)</li>
 *   <li>{@code source_type}     — 위기가 발생한 콘텐츠 유형 (POST / COMMENT)</li>
 *   <li>{@code source_id}       — 위기가 감지된 게시글 또는 댓글 ID</li>
 *   <li>{@code is_read}         — 가디언의 알림 확인 여부</li>
 * </ul>
 *
 * <p>⚠️ 익명성 보호:
 * triggerUserId는 PK만 저장하며, 닉네임·deviceId 등 식별 가능 데이터는 포함하지 않는다.
 */
@Entity
@Table(
    name = "guardian_alerts",
    indexes = @Index(name = "idx_guardian_alerts_guardian_id", columnList = "guardian_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianAlert extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 수신할 가디언의 userId(PK).
     * 가디언 1명당 알림 레코드 1개가 생성된다.
     */
    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    /**
     * 위기 콘텐츠를 작성한 사용자 userId(PK).
     * 가디언이 개입 대상을 파악하기 위해 저장.
     */
    @Column(name = "trigger_user_id", nullable = false)
    private Long triggerUserId;

    /**
     * 위기가 발생한 콘텐츠 유형.
     * POST — 게시글, COMMENT — 댓글
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CrisisSourceType sourceType;

    /**
     * 위기가 감지된 게시글 또는 댓글 ID.
     * sourceType과 함께 사용하여 원본 콘텐츠를 추적한다.
     */
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /**
     * 알림 읽음 여부.
     * 가디언이 알림 목록을 조회하면 true로 변경된다.
     */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 가디언 알림 레코드를 생성한다.
     *
     * @param guardianId     알림 수신 가디언 userId
     * @param triggerUserId  위기 콘텐츠 작성자 userId
     * @param sourceType     콘텐츠 유형 (POST / COMMENT)
     * @param sourceId       게시글 또는 댓글 ID
     * @return 저장 준비된 GuardianAlert 인스턴스
     */
    public static GuardianAlert create(Long guardianId, Long triggerUserId,
                                       CrisisSourceType sourceType, Long sourceId) {
        GuardianAlert alert = new GuardianAlert();
        alert.guardianId = guardianId;
        alert.triggerUserId = triggerUserId;
        alert.sourceType = sourceType;
        alert.sourceId = sourceId;
        alert.isRead = false;
        return alert;
    }

    // ─────────────────────────────────────────────────────────────────
    // 비즈니스 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 알림을 읽음 처리한다.
     * 가디언이 알림 목록 API를 호출하면 자동으로 호출된다.
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
