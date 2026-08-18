package com.hackathon.skinroutine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 하루 1개의 기록 (사진 + 상태 3개). 클래스명은 record가 Java 예약어라 DailyRecord.
 * (user_id, record_date) UNIQUE — "사용자당 하루 1기록"을 DB 레벨에서 최종 보장 (docs/05).
 */
@Entity
@Table(name = "records", uniqueConstraints =
        @UniqueConstraint(name = "uk_records_user_date", columnNames = {"user_id", "record_date"}))
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate recordDate; // KST(Asia/Seoul) 기준 — KoreaTime.today()로만 만들 것

    @Column(length = 1000)
    private String photoUrl; // 업로드 실패 시 null 허용 — 사진이 죽어도 기록·분석은 살린다

    @Column(nullable = false)
    private Double sleepHours; // 상태 입력 ① 수면 시간

    @Column(nullable = false)
    private Boolean hadDrinkOrSnack; // 상태 입력 ② 음주·야식

    @Column(nullable = false)
    private Integer stressLevel; // 상태 입력 ③ 스트레스 1~3

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected DailyRecord() {}

    public DailyRecord(User user, LocalDate recordDate, String photoUrl,
                       Double sleepHours, Boolean hadDrinkOrSnack, Integer stressLevel) {
        this.user = user;
        this.recordDate = recordDate;
        this.photoUrl = photoUrl;
        this.sleepHours = sleepHours;
        this.hadDrinkOrSnack = hadDrinkOrSnack;
        this.stressLevel = stressLevel;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Double getSleepHours() {
        return sleepHours;
    }

    public Boolean getHadDrinkOrSnack() {
        return hadDrinkOrSnack;
    }

    public Integer getStressLevel() {
        return stressLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
