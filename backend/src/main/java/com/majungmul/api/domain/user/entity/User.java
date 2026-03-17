package com.majungmul.api.domain.user.entity;

import com.majungmul.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 서비스 사용자 엔티티.
 *
 * <p>간이역 프로젝트의 핵심 설계 원칙: <b>익명성 우선</b>.
 * 고립 청년들은 이메일, 전화번호 등 개인 정보 없이도 즉시 서비스를 이용할 수 있다.
 * 식별자는 클라이언트가 생성한 기기 고유 ID(deviceId)만 사용한다.
 *
 * <p>DB 테이블: {@code users}
 *
 * <p>주요 컬럼:
 * <ul>
 *   <li>{@code device_id} — 기기 고유 식별자. 재설치 시 새 익명 계정이 생성됨.</li>
 *   <li>{@code nickname}  — 선택적 닉네임. 커뮤니티 활동 시 등록 권장.</li>
 *   <li>{@code role}      — 초기값 ANONYMOUS, 활동에 따라 USER로 승격 가능.</li>
 *   <li>{@code is_deleted}— 물리 삭제 대신 논리 삭제(soft delete)로 데이터 보존.</li>
 * </ul>
 *
 * <p>⚠️ 익명성 보호: 로그에 deviceId, nickname 등 식별 가능 데이터를 직접 출력하지 말 것.
 */
@Entity
@Table(
    name = "users",
    indexes = @Index(name = "idx_users_device_id", columnList = "device_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용 기본 생성자 — 외부 직접 생성 방지
@ToString(exclude = {"deviceId", "nickname"})       // ⚠️ 익명성 보호: 로그 출력 시 개인 식별 필드 제외
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기기 고유 식별자 (클라이언트 생성 UUID).
     * 이 값이 익명 사용자를 식별하는 유일한 키이므로 unique 제약.
     */
    @Column(name = "device_id", unique = true, nullable = false, length = 100)
    private String deviceId;

    /**
     * 선택적 닉네임 — null 허용.
     * 커뮤니티 게시글 작성 시 설정을 유도하지만 강제하지 않는다.
     */
    @Column(name = "nickname", length = 20)
    private String nickname;

    /**
     * 사용자 역할.
     * 기본값: ANONYMOUS → 닉네임 등록 후 USER로 승격.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * 보유 포인트.
     * 미션 완료 시 {@code MissionService → UserService.addPoint()} 경로로 적립된다.
     * 상품권 교환 등 소비 시 {@code deductPoint()} 로 차감 — 잔액 부족이면 INSUFFICIENT_POINTS 예외.
     */
    @Column(name = "point", nullable = false)
    private int point = 0;

    /**
     * 논리 삭제 플래그.
     * true이면 탈퇴 처리된 계정 — 물리 삭제 없이 데이터를 보존하여 통계·감사 추적에 활용.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ─────────────────────────────────────────────────────────────────
    // 정적 팩토리 메서드 (생성자 대신 사용 — 의도를 명확히 표현)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 익명 사용자를 생성한다.
     *
     * <p>서비스 첫 진입 시 호출됨. deviceId만으로 계정이 생성되며,
     * 닉네임·이메일 등 추가 정보는 요구하지 않는다.
     *
     * @param deviceId 클라이언트가 생성한 기기 고유 ID
     * @return ANONYMOUS 역할의 새 User 인스턴스
     */
    public static User createAnonymous(String deviceId) {
        User user = new User();
        user.deviceId = deviceId;
        user.role = UserRole.ANONYMOUS;
        user.isDeleted = false;
        return user;
    }

    // ─────────────────────────────────────────────────────────────────
    // 비즈니스 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 닉네임을 등록하고 역할을 USER로 승격한다.
     *
     * <p>ANONYMOUS 사용자가 커뮤니티 참여 의사를 밝힐 때 호출.
     * 닉네임 등록만으로도 서비스에 '소속감'을 느낄 수 있도록 설계.
     *
     * @param nickname 등록할 닉네임 (최대 20자)
     */
    public void registerNickname(String nickname) {
        this.nickname = nickname;
        this.role = UserRole.USER;
    }

    /**
     * 포인트를 적립한다.
     *
     * <p>미션 완료 시 {@code MissionService}가 호출한다.
     * 음수 amount를 방어하기 위해 검증을 포함한다.
     *
     * @param amount 적립할 포인트 (양수)
     * @throws IllegalArgumentException amount 가 0 이하인 경우
     */
    public void addPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 포인트는 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    /**
     * 포인트를 차감한다.
     *
     * <p>상품권 교환 등 소비 요청 시 호출된다.
     * 잔액이 부족하면 호출부에서 INSUFFICIENT_POINTS 예외를 던진다 —
     * 엔티티는 예외 타입(도메인 레이어 의존)을 직접 알지 않도록 설계.
     *
     * @param amount 차감할 포인트 (양수)
     * @return 차감 후 잔액이 0 이상이면 true, 부족하면 false
     */
    public boolean deductPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 포인트는 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            return false; // 잔액 부족 — 호출부(UserService)가 INSUFFICIENT_POINTS 예외 처리
        }
        this.point -= amount;
        return true;
    }

    /**
     * 계정을 논리 삭제 처리한다 (탈퇴).
     * 물리 삭제를 하지 않는 이유: 미션·게시글 등 연관 데이터의 무결성 보존.
     */
    public void withdraw() {
        this.isDeleted = true;
    }
}
