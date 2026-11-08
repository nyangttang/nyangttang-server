package com.nyangttang.server.study.repository;

import com.nyangttang.server.study.entity.DailyStudyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStudyStatRepository extends JpaRepository<DailyStudyStat, Long> {
    Optional<DailyStudyStat> findByMember_IdAndStudyDate(Long memberId, LocalDate studyDate);
    List<DailyStudyStat> findAllByMember_Room_IdAndStudyDate(Long roomId, LocalDate studyDate);
}
