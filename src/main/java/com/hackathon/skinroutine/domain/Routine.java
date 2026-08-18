package com.hackathon.skinroutine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 그날 제안한 루틴. "다른 루틴 보기"(#9) 시 generation+1로 새 행이 쌓이고 이전 것은 보관.
 * completed_at 은 6-2 신뢰 가정 검증의 측정 데이터 (docs/05).
 */
@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private DailyRecord record;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 300)
    private String reasonText; // 오늘 이 루틴인 이유

    @Column(length = 500)
    private String methodText; // 실행 방법

    @Column(nullable = false)
    private Integer expectedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineStatus status = RoutineStatus.SUGGESTED;

    @Column(nullable = false)
    private Integer generation = 1; // 재추천 회차

    private Instant completedAt; // null 허용 — 완료 시각

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected Routine() {}

    public Routine(User user, DailyRecord record, String title, String reasonText,
                   String methodText, Integer expectedMinutes, Integer generation) {
        this.user = user;
        this.record = record;
        this.title = title;
        this.reasonText = reasonText;
        this.methodText = methodText;
        this.expectedMinutes = expectedMinutes;
        this.generation = generation;
    }

    /** #7 완료 체크 — B의 RoutineController에서 이 메서드를 호출하면 된다 */
    public void markCompleted() {
        markCompleted(Instant.now());
    }

    /** 시드 데이터처럼 완료 시각을 지정해야 할 때 사용 */
    public void markCompleted(Instant when) {
        this.status = RoutineStatus.COMPLETED;
        this.completedAt = when;
    }

    /** #8 "나중에 실천할게요" */
    public void markDeferred() {
        this.status = RoutineStatus.DEFERRED;
        this.completedAt = null;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public DailyRecord getRecord() {
        return record;
    }

    public String getTitle() {
        return title;
    }

    public String getReasonText() {
        return reasonText;
    }

    public String getMethodText() {
        return methodText;
    }

    public Integer getExpectedMinutes() {
        return expectedMinutes;
    }

    public RoutineStatus getStatus() {
        return status;
    }

    public Integer getGeneration() {
        return generation;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
