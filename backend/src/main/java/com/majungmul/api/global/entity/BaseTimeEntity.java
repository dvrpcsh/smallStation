package com.majungmul.api.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티가 상속하는 공통 시간 정보 기반 클래스.
 *
 * <p>JpaConfig의 @EnableJpaAuditing 설정과 함께 동작하여
 * 생성·수정 시각을 자동으로 기록한다.
 *
 * <p>사용법:
 * <pre>
 * {@literal @}Entity
 * public class User extends BaseTimeEntity { ... }
 * </pre>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /** 레코드 최초 생성 시각 — INSERT 시 자동 설정, 이후 변경 불가 */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 마지막 수정 시각 — UPDATE 시마다 자동 갱신 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
