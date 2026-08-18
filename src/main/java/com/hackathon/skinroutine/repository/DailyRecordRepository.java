package com.hackathon.skinroutine.repository;

import com.hackathon.skinroutine.domain.DailyRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    /** 오늘 기록 찾기 — 날짜는 반드시 KoreaTime.today()로 넘길 것 */
    Optional<DailyRecord> findByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    boolean existsByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    /** #5 기록 목록 — Pageable(PageRequest.of(0, limit))로 개수 제한 */
    List<DailyRecord> findByUserIdOrderByRecordDateDesc(UUID userId, Pageable pageable);

    /** 스트릭 계산용 — 기록 날짜만 최신순으로 */
    @Query("select r.recordDate from DailyRecord r where r.user.id = :userId order by r.recordDate desc")
    List<LocalDate> findRecordDatesDesc(@Param("userId") UUID userId, Pageable pageable);
}
