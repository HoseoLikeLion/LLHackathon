package com.hackathon.skinroutine.repository;

import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.RoutineStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    /**
     * "오늘의 루틴" = 오늘 record에 달린 루틴 중 최신 generation 1개.
     * B의 #6·7·8 구현에서 이 메서드를 쓰면 된다 (재추천 후에도 항상 최신 것이 잡힘).
     */
    Optional<Routine> findTopByRecordIdOrderByGenerationDesc(Long recordId);

    /** 오늘 쌓인 루틴 전부 (#9 재추천 시 이전 제목 제외용) */
    List<Routine> findByRecordId(Long recordId);

    /** 최근 제안한 루틴 — AI 프롬프트에서 같은 제목 반복 회피용 */
    List<Routine> findTop10ByUserIdOrderByIdDesc(UUID userId);

    /** #10 리포트 효과 집계 — 구간 내 완료된 루틴 (record까지 한 번에) */
    @Query("""
            select ro from Routine ro join fetch ro.record r
            where ro.user.id = :userId and ro.status = :status and r.recordDate >= :from""")
    List<Routine> findByStatusInRange(@Param("userId") UUID userId,
                                      @Param("status") RoutineStatus status,
                                      @Param("from") LocalDate from);
}
