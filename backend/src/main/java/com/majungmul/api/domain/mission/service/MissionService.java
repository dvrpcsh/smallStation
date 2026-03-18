package com.majungmul.api.domain.mission.service;

import com.majungmul.api.domain.mission.dto.MissionCompleteResponse;
import com.majungmul.api.domain.mission.dto.MissionResponse;
import com.majungmul.api.domain.mission.entity.Mission;
import com.majungmul.api.domain.mission.entity.UserMission;
import com.majungmul.api.domain.mission.repository.MissionRepository;
import com.majungmul.api.domain.mission.repository.UserMissionRepository;
import com.majungmul.api.domain.user.dto.UserResponse;
import com.majungmul.api.domain.user.service.UserService;
import com.majungmul.api.global.common.exception.BusinessException;
import com.majungmul.api.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 미션(Mission) 비즈니스 로직 서비스.
 *
 * <p>담당 기능:
 * <ol>
 *   <li>활성 미션 목록 조회</li>
 *   <li>미션 완료 처리 — UserMission 인증 로그 저장 후 {@code UserService.addPoint()} 호출</li>
 * </ol>
 *
 * <p>트랜잭션 원자성 보장:
 * <pre>
 * completeMission() [@Transactional]
 *   ① UserMission 인증 로그 저장      (현재 트랜잭션 내)
 *   ② UserService.addPoint() 호출    (PROPAGATION.REQUIRED → 동일 트랜잭션 참여)
 *   → 둘 중 하나라도 실패하면 전체 롤백, 포인트와 인증 로그는 항상 함께 저장되거나 함께 취소된다.
 * </pre>
 *
 * <p>⚠️ 익명성 보호: 로그에 닉네임 등 식별 정보 출력 금지. userId(PK)만 허용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────
    // 미션 목록 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 활성화된 미션 목록을 조회한다.
     *
     * <p>is_active=true 인 미션만 반환한다.
     * 클라이언트는 이 목록을 기반으로 오늘 수행할 미션을 선택한다.
     *
     * @return 활성 미션 응답 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<MissionResponse> getActiveMissions() {
        List<Mission> missions = missionRepository.findAllByIsActiveTrue();
        log.info("[Mission] 활성 미션 목록 조회. count={}", missions.size());
        return missions.stream()
                .map(MissionResponse::from)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // 미션 완료 처리 (핵심 — 원자적 트랜잭션)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 미션 완료를 처리하고 포인트를 지급한다.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. 미션 존재 및 활성 상태 확인        → M001
     * 2. 오늘 이미 완료했는지 중복 확인    → M003
     * 3. UserMission 인증 로그 저장        (트랜잭션 ①)
     * 4. UserService.addPoint() 호출       (트랜잭션 ① 참여 — 원자적)
     * 5. 완료 결과 반환
     * </pre>
     *
     * <p>실패 시나리오:
     * <ul>
     *   <li>미션 없음/비활성: MISSION_NOT_FOUND (M001) — 로그 WARN</li>
     *   <li>오늘 이미 완료: MISSION_ALREADY_DONE (M003) — 로그 WARN</li>
     *   <li>사용자 없음: USER_NOT_FOUND (U001) — UserService에서 throw, 로그 ERROR</li>
     *   <li>기타 예외: 전체 트랜잭션 롤백 — 로그 ERROR</li>
     * </ul>
     *
     * @param userId    미션을 완료한 사용자 PK (JWT에서 추출)
     * @param missionId 완료할 미션 PK
     * @return 완료 결과 (적립 포인트, 누적 포인트 포함)
     * @throws BusinessException MISSION_NOT_FOUND    — 존재하지 않거나 비활성화된 미션
     * @throws BusinessException MISSION_ALREADY_DONE — 오늘 이미 완료한 미션
     * @throws BusinessException USER_NOT_FOUND       — 존재하지 않는 사용자
     */
    @Transactional
    public MissionCompleteResponse completeMission(Long userId, Long missionId) {

        // ① 미션 존재 및 활성 상태 확인
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> {
                    log.warn("[Mission] 완료 실패 — 존재하지 않는 미션. userId={}, missionId={}", userId, missionId);
                    return new BusinessException(ErrorCode.MISSION_NOT_FOUND);
                });

        if (!mission.isActive()) {
            log.warn("[Mission] 완료 실패 — 비활성화된 미션. userId={}, missionId={}", userId, missionId);
            throw new BusinessException(ErrorCode.MISSION_NOT_FOUND);
        }

        // ② 오늘 이미 완료했는지 중복 확인 (하루 1회 제한)
        LocalDate today = LocalDate.now();
        if (userMissionRepository.existsByUserIdAndMissionIdAndCompletedDate(userId, missionId, today)) {
            log.warn("[Mission] 완료 실패 — 오늘 이미 완료한 미션. userId={}, missionId={}, date={}", userId, missionId, today);
            throw new BusinessException(ErrorCode.MISSION_ALREADY_DONE);
        }

        // ③ UserMission 인증 로그 저장
        UserMission userMission = UserMission.create(userId, missionId, today);
        userMissionRepository.save(userMission);
        log.info("[Mission] 인증 로그 저장 완료. userId={}, missionId={}, date={}", userId, missionId, today);

        // ④ 포인트 적립 (같은 트랜잭션 내 — UserService.addPoint() 는 PROPAGATION.REQUIRED 로 합류)
        //    인증 로그 저장과 포인트 적립은 원자적으로 처리된다. 어느 한쪽이 실패하면 전체 롤백.
        try {
            userService.addPoint(userId, mission.getRewardAmount());
        } catch (Exception e) {
            log.error("[Mission] 포인트 적립 실패 — 트랜잭션 롤백. userId={}, missionId={}, rewardAmount={}, error={}",
                    userId, missionId, mission.getRewardAmount(), e.getMessage());
            throw e; // 예외 재던짐 → @Transactional 이 전체 롤백 처리
        }

        // ⑤ 적립 후 사용자 누적 포인트 조회 (같은 트랜잭션 내 — 영속성 컨텍스트의 최신 상태 반영)
        UserResponse updatedUser = userService.getMyInfo(userId);

        log.info("[Mission] 완료 처리 성공. userId={}, missionId={}, rewardAmount={}, totalPoints={}",
                userId, missionId, mission.getRewardAmount(), updatedUser.point());

        return MissionCompleteResponse.of(mission, updatedUser.point());
    }
}
