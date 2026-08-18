package com.hackathon.skinroutine.repository;

import com.hackathon.skinroutine.domain.Analysis;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByRecordId(Long recordId);

    /**
     * 직전 분석(특정 날짜 이전의 최신) — 비전 프롬프트의 "전일 대비" 맥락용. Pageable(0,1)로 1개만.
     * join fetch: 트랜잭션 밖에서 a.getRecord()를 읽어도 LAZY 예외가 안 나게 record까지 함께 로드.
     */
    @Query("""
            select a from Analysis a join fetch a.record r
            where r.user.id = :userId and r.recordDate < :before
            order by r.recordDate desc""")
    List<Analysis> findLatestBefore(@Param("userId") UUID userId,
                                    @Param("before") LocalDate before, Pageable pageable);

    /** #10 리포트 구간 조회 — record까지 한 번에 가져와 N+1 방지 */
    @Query("""
            select a from Analysis a join fetch a.record r
            where r.user.id = :userId and r.recordDate >= :from
            order by r.recordDate asc""")
    List<Analysis> findInRangeWithRecord(@Param("userId") UUID userId, @Param("from") LocalDate from);
}
