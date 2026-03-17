package com.majungmul.api.domain.guardian.listener;

import com.majungmul.api.domain.guardian.entity.GuardianAlert;
import com.majungmul.api.domain.guardian.event.CrisisDetectedEvent;
import com.majungmul.api.domain.guardian.repository.GuardianAlertRepository;
import com.majungmul.api.domain.user.entity.User;
import com.majungmul.api.domain.user.entity.UserRole;
import com.majungmul.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 위기 감지 이벤트 수신 및 가디언 알림 처리 리스너.
 *
 * <p>처리 흐름:
 * <pre>
 * CrisisDetectedEvent 수신 (비동기)
 *   → 1. GUARDIAN 역할 사용자 전체 조회
 *   → 2. 가디언별 GuardianAlert DB 저장
 *   → 3. 가디언별 알림 로그 출력 (현재: 시뮬레이션)
 *        Phase 4에서 실제 푸시 알림(FCM 등) 또는 WebSocket으로 교체 가능
 * </pre>
 *
 * <p>@Async 설계 의도:
 * 게시글·댓글 저장 트랜잭션이 완료된 후 별도 스레드에서 알림 처리를 수행한다.
 * 알림 저장 실패 시 로그만 남기고 메인 요청에는 영향을 주지 않는다.
 *
 * <p>⚠️ 익명성 보호: 로그에 matchedKeywords나 콘텐츠 원문을 출력하지 않는다.
 * guardianId와 triggerUserId(PK)만 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianAlertListener {

    private final UserRepository userRepository;
    private final GuardianAlertRepository guardianAlertRepository;

    /**
     * 위기 감지 이벤트를 비동기로 처리한다.
     *
     * <p>활성 가디언이 없는 경우 WARN 로그를 남기고 종료한다.
     * 가디언 1명당 GuardianAlert 레코드 1개가 생성된다.
     *
     * @param event 세이프티 필터가 발행한 위기 감지 이벤트
     */
    @Async
    @Transactional
    @EventListener
    public void handleCrisisDetected(CrisisDetectedEvent event) {

        // ① 활성 가디언 전체 조회
        List<User> guardians = userRepository.findAllByRoleAndIsDeletedFalse(UserRole.GUARDIAN);

        if (guardians.isEmpty()) {
            log.warn("[Guardian] 활성 가디언이 없습니다. 위기 알림을 전송할 수 없습니다. "
                    + "sourceType={}, sourceId={}", event.sourceType(), event.sourceId());
            return;
        }

        // ② 가디언별 알림 레코드 저장 + 알림 시뮬레이션
        for (User guardian : guardians) {
            GuardianAlert alert = GuardianAlert.create(
                    guardian.getId(),
                    event.triggerUserId(),
                    event.sourceType(),
                    event.sourceId()
            );
            guardianAlertRepository.save(alert);

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // [알림 전송 확장 포인트] — Phase 4에서 실제 알림으로 교체 예정
            //
            // 현재: DB 저장 + 로그 출력으로 알림 전송을 시뮬레이션.
            // 향후 다음 방식 중 하나로 교체:
            //   1. FCM(Firebase Cloud Messaging) 푸시 알림
            //      fcmService.send(guardian.getDeviceId(), alertPayload)
            //   2. WebSocket / SSE (실시간 알림 피드)
            //   3. 이메일 / SMS (가디언이 별도 연락처 등록한 경우)
            //
            // ⚠️ 실제 구현 시 가디언의 연락처(deviceId, 이메일 등)는
            //    별도 암호화 컬럼으로 관리하며 로그에 출력하지 않는다.
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.info("[Guardian] 위기 알림 전송 완료 (시뮬레이션). "
                    + "guardianId={}, sourceType={}, sourceId={}",
                    guardian.getId(), event.sourceType(), event.sourceId());
        }

        log.info("[Guardian] 위기 알림 처리 완료. guardianCount={}, sourceType={}, sourceId={}",
                guardians.size(), event.sourceType(), event.sourceId());
    }
}
