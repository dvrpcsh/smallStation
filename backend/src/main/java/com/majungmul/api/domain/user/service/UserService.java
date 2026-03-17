package com.majungmul.api.domain.user.service;

import com.majungmul.api.domain.user.dto.UserResponse;
import com.majungmul.api.domain.user.dto.UserUpdateRequest;
import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.repository.UserRepository;
import com.majungmul.api.global.common.exception.BusinessException;
import com.majungmul.api.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자(User) 비즈니스 로직 서비스.
 *
 * <p>담당 기능:
 * <ol>
 *   <li>현재 로그인 사용자 프로필 조회</li>
 *   <li>닉네임 수정 (Dirty Checking 활용 — 별도 save() 호출 불필요)</li>
 *   <li>포인트 적립 — 미션 완료 시 {@code MissionService}에서 호출</li>
 *   <li>포인트 차감 — 상품권 교환 시 {@code RewardService}에서 호출, 잔액 부족 시 예외</li>
 * </ol>
 *
 * <p>포인트 흐름:
 * <pre>
 * MissionService.completeMission()
 *   → UserService.addPoint(userId, amount)      // 미션 완료 적립
 *
 * RewardService.exchangeVoucher()
 *   → UserService.deductPoint(userId, amount)   // 상품권 교환 차감
 * </pre>
 *
 * <p>⚠️ 익명성 보호: 로그에 deviceId·닉네임을 직접 출력하지 않는다.
 * 로그에는 userId(PK)와 역할 정도만 허용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────
    // 사용자 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * userId로 활성 사용자를 조회하고 응답 DTO를 반환한다.
     *
     * <p>탈퇴(isDeleted=true)된 계정은 조회 대상에서 제외된다.
     *
     * @param userId JWT에서 추출된 사용자 PK
     * @return 사용자 프로필 응답 DTO
     * @throws BusinessException USER_NOT_FOUND — 존재하지 않거나 탈퇴한 사용자
     */
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = findActiveUser(userId);
        return UserResponse.from(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // 닉네임 수정
    // ─────────────────────────────────────────────────────────────────

    /**
     * 닉네임을 수정한다.
     *
     * <p>처리 흐름:
     * <pre>
     * 1. 닉네임 중복 검사 (U003)
     * 2. User.registerNickname() 호출 → Dirty Checking으로 자동 UPDATE
     *    - ANONYMOUS → USER 역할 승격 포함
     * </pre>
     *
     * <p>Dirty Checking: @Transactional 범위 내에서 엔티티 필드를 수정하면
     * 트랜잭션 종료 시점에 JPA가 변경 감지(Dirty Checking)하여 UPDATE 쿼리를 자동 실행한다.
     * 별도 {@code save()} 호출 없이 영속성 컨텍스트가 동기화를 처리한다.
     *
     * @param userId  JWT에서 추출된 사용자 PK
     * @param request 변경할 닉네임을 담은 요청 DTO
     * @return 수정된 사용자 프로필 응답 DTO
     * @throws BusinessException USER_NOT_FOUND      — 존재하지 않거나 탈퇴한 사용자
     * @throws BusinessException NICKNAME_DUPLICATED — 이미 사용 중인 닉네임
     */
    @Transactional
    public UserResponse updateNickname(Long userId, UserUpdateRequest request) {

        // ① 닉네임 중복 검사 — 본인의 현재 닉네임과 동일하더라도 허용 (변경 없음 케이스)
        boolean isDuplicated = userRepository.existsByNicknameAndIsDeletedFalse(request.nickname());
        if (isDuplicated) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // ② 닉네임 등록 및 역할 승격 (ANONYMOUS → USER)
        // Dirty Checking: 트랜잭션 종료 시 JPA가 변경 감지 → UPDATE 자동 실행
        User user = findActiveUser(userId);
        user.registerNickname(request.nickname());

        log.info("[User] 닉네임 수정 완료. userId={}, role={}", userId, user.getRole());

        return UserResponse.from(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // 포인트 적립 (MissionService에서 호출)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 포인트를 적립한다.
     *
     * <p>미션 완료 시 {@code MissionService.completeMission()}에서 호출된다.
     * 두 서비스가 같은 트랜잭션 안에서 동작하므로 원자적으로 처리된다.
     *
     * @param userId 포인트를 적립받을 사용자 PK
     * @param amount 적립할 포인트 (양수)
     * @throws BusinessException USER_NOT_FOUND — 존재하지 않거나 탈퇴한 사용자
     */
    @Transactional
    public void addPoint(Long userId, int amount) {
        User user = findActiveUser(userId);
        user.addPoint(amount); // Dirty Checking — 트랜잭션 종료 시 UPDATE 자동 실행
        log.info("[User] 포인트 적립 완료. userId={}, amount={}, total={}", userId, amount, user.getPoint());
    }

    // ─────────────────────────────────────────────────────────────────
    // 포인트 차감 (RewardService에서 호출)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 포인트를 차감한다.
     *
     * <p>상품권 교환 등 포인트 소비 시 {@code RewardService}에서 호출된다.
     * 잔액이 부족하면 {@link ErrorCode#INSUFFICIENT_POINTS}(U004) 예외를 발생시킨다.
     *
     * @param userId 포인트를 차감할 사용자 PK
     * @param amount 차감할 포인트 (양수)
     * @throws BusinessException USER_NOT_FOUND    — 존재하지 않거나 탈퇴한 사용자
     * @throws BusinessException INSUFFICIENT_POINTS — 잔액 부족
     */
    @Transactional
    public void deductPoint(Long userId, int amount) {
        User user = findActiveUser(userId);
        boolean succeeded = user.deductPoint(amount); // Dirty Checking — 트랜잭션 종료 시 UPDATE 자동 실행
        if (!succeeded) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        log.info("[User] 포인트 차감 완료. userId={}, amount={}, remaining={}", userId, amount, user.getPoint());
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 공통 메서드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 활성 사용자를 조회한다. 탈퇴 계정이면 USER_NOT_FOUND 예외를 발생시킨다.
     *
     * <p>서비스 내 여러 메서드에서 반복되는 "조회 + 탈퇴 여부 확인" 로직을 단일 메서드로 집약.
     *
     * @param userId 조회할 사용자 PK
     * @return 활성 상태의 User 엔티티
     * @throws BusinessException USER_NOT_FOUND
     */
    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
