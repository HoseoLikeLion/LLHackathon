package com.hackathon.skinroutine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * 익명 사용자 — 로그인 없음(6-3 확정).
 * 서버가 발급한 uuid를 클라이언트가 localStorage에 보관하고, 이후 X-User-Id 헤더로 보낸다.
 */
@Entity
@Table(name = "users") // ⚠️ user는 Postgres 예약어라 테이블명은 users (docs/05)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 30)
    private String nickname;

    @Column(nullable = false)
    private boolean isDemo = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected User() {} // JPA 기본 생성자

    public User(String nickname, boolean isDemo) {
        this.nickname = nickname;
        this.isDemo = isDemo;
    }

    public UUID getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
