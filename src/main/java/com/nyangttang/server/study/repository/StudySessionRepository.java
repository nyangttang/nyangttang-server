package com.nyangttang.server.study.repository;

import com.nyangttang.server.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    Optional<StudySession> findByMember_IdAndEndedAtIsNull(Long memberId);

    String SUMMARY_SELECT = """
            select s.id as id, s.member.room.id as roomId, s.studyDate as studyDate,
                   s.startedAt as startedAt, s.endedAt as endedAt,
                   s.endReason as endReason, s.creditedMs as creditedMs
            from StudySession s
            """;

    @org.springframework.data.jpa.repository.Query(SUMMARY_SELECT + """
            where s.member.id = :memberId and s.studyDate = :date
            order by s.id desc
            """)
    java.util.List<StudySessionSummary> findSummaryPage(
            @org.springframework.data.repository.query.Param("memberId") Long memberId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            org.springframework.data.domain.Pageable page);

    @org.springframework.data.jpa.repository.Query(SUMMARY_SELECT + """
            where s.member.id = :memberId and s.studyDate = :date and s.id < :beforeId
            order by s.id desc
            """)
    java.util.List<StudySessionSummary> findSummaryPageBefore(
            @org.springframework.data.repository.query.Param("memberId") Long memberId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("beforeId") Long beforeId,
            org.springframework.data.domain.Pageable page);

    Optional<StudySession> findByIdAndMember_User_Id(Long id, Long userId);
}
