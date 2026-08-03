package com.tmsproject.global.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티가 공통으로 갖는 생성/수정 일시를 관리하는 상위 클래스.
 * 각 엔티티는 이 클래스를 extends 하기만 하면 createdAt/updatedAt이 자동으로 채워진다.
 *
 * 주의: 이 기능이 동작하려면 메인 애플리케이션 클래스나 별도 설정 클래스에
 * @EnableJpaAuditing 어노테이션을 추가해야 한다.
 */
@Getter
@MappedSuperclass // 다른 테이블 상속만 해주는 클래스
@EntityListeners(AuditingEntityListener.class) // 값을 자동으로 채워주는 감시자 (있어야 @CreatedDate 등이 동작)
public abstract class BaseEntity {

    @CreatedDate // 생성할 때, 자동으로 값이 채워지는 필드
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정할 때, 값이 자동으로 채워진다.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
