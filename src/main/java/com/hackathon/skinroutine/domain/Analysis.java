package com.hackathon.skinroutine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** 그날의 AI 분석 결과 — 기록당 1개 (record_id UNIQUE). 같은 record 재분석 금지(비용). */
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    private DailyRecord record;

    @Column(nullable = false)
    private Integer score; // 0~100 (화면 ②의 "72점")

    @Column(nullable = false)
    private Integer redness; // 1~5, 높을수록 심함

    @Column(nullable = false)
    private Integer moisture; // 1~5, 높을수록 좋음

    @Column(nullable = false)
    private Integer oil; // 1~5, 높을수록 많음

    // JSON 배열 문자열 — H2·Postgres 겸용을 위해 json 타입 대신 varchar에 직렬화해 저장
    @Column(name = "labels", length = 1000)
    private String labelsJson;

    @Column(length = 500)
    private String insightText; // 상관 코멘트 1줄

    @Column(nullable = false)
    private boolean isFallback; // AI 실패로 룰 기반 폴백 응답이었는지

    @Column(length = 20000)
    private String raw; // OpenAI 원본 응답 (디버깅용, 저장 전 20000자로 자름)

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected Analysis() {}

    public Analysis(DailyRecord record, Integer score, Integer redness, Integer moisture, Integer oil,
                    String labelsJson, String insightText, boolean isFallback, String raw) {
        this.record = record;
        this.score = score;
        this.redness = redness;
        this.moisture = moisture;
        this.oil = oil;
        this.labelsJson = labelsJson;
        this.insightText = insightText;
        this.isFallback = isFallback;
        this.raw = raw;
    }

    public Long getId() {
        return id;
    }

    public DailyRecord getRecord() {
        return record;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getRedness() {
        return redness;
    }

    public Integer getMoisture() {
        return moisture;
    }

    public Integer getOil() {
        return oil;
    }

    public String getLabelsJson() {
        return labelsJson;
    }

    public String getInsightText() {
        return insightText;
    }

    public boolean isFallback() {
        return isFallback;
    }

    public String getRaw() {
        return raw;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
