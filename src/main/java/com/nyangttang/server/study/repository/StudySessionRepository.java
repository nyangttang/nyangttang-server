package com.nyangttang.server.study.repository;

import com.nyangttang.server.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    Optional<StudySession> findByMember_IdAndEndedAtIsNull(Long memberId);
}
