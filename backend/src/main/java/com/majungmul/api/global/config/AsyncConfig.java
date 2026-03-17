package com.majungmul.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정.
 *
 * <p>@EnableAsync를 활성화하여 @Async 어노테이션이 붙은 메서드를
 * 별도 스레드에서 실행할 수 있도록 한다.
 *
 * <p>주요 사용처:
 * <ul>
 *   <li>{@code GuardianAlertListener} — 위기 이벤트 수신 및 알림 처리를
 *       메인 트랜잭션(게시글·댓글 저장)과 분리하여 알림 실패가 사용자 요청에 영향을 주지 않도록 함.</li>
 * </ul>
 *
 * <p>기본 스레드 풀(SimpleAsyncTaskExecutor)을 사용한다.
 * 트래픽 증가 시 {@code ThreadPoolTaskExecutor}로 교체하여 스레드 수 제어 권장.
 */
@EnableAsync
@Configuration
public class AsyncConfig {
}
