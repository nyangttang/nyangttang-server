package com.nyangttang.server.study.repository;

import com.nyangttang.server.study.entity.StudyEndReason;
import java.time.Instant;
import java.time.LocalDate;

// Scalar projection: listing sessions must not load start/end snapshot JSON.
public interface StudySessionSummary {
    Long getId();
    Long getRoomId();
    LocalDate getStudyDate();
    Instant getStartedAt();
    Instant getEndedAt();
    StudyEndReason getEndReason();
    Long getCreditedMs();
}
