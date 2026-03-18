package com.majungmul.api.domain.mission.entity;

import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 미션 완료 인증 로그(UserMission) 엔티티.
 *
 * <p>어떤 유저가 어떤 미션을 언제 완료했는지 기록한다.
 * 동일한 미션은 하루에 한 번만 완료할 수 있도록 {@code (user_id, mission_id, completed_date)}
 * 복합 유니크 제약을 적용한다.
 *
 * <p>DB 테이블: {@code user_missions}
 *
 * <p>⚠️ 설계 원칙: 도메인 간 직접 엔티티 참조 금지.
 * User 엔티티와 Mission 엔티티를 직접 참조하지 않고 ID(PK)만 저장한다.
 * (CLAUDE.md §코드 아키텍처 > Mission → Reward 연동 흐름 참고)
 */
@Entity
@Table(
    name = "user_missions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_mission_date",
        columnNames = {"user_id", "mission_id", "completed_date"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 미션을 완료한 사용자 PK.
     * User 엔티티를 직접 참조하지 않고 ID만 저장 (도메인 결합도 최소화).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 완료한 미션 PK.
     * Mission 엔티티를 직접 참조하지 않고 ID만 저장.
     */
    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    /**
     * 미션 완료 날짜.
     * 같은 날짜에 동일한 미션을 두 번 완료할 수 없도록 유니크 제약에 포함.
     */
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 미션 완료 기록을 생성한다.
     *
     * @param userId        완료한 사용자 PK
     * @param missionId     완료한 미션 PK
     * @param completedDate 완료 날짜
     * @return 새 UserMission 인스턴스
     */
    public static UserMission create(Long userId, Long missionId, LocalDate completedDate) {
        UserMission userMission = new UserMission();
        userMission.userId = userId;
        userMission.missionId = missionId;
        userMission.completedDate = completedDate;
        return userMission;
    }
}
