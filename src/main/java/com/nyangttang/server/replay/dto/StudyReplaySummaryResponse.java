package com.nyangttang.server.replay.dto;

import com.nyangttang.server.study.entity.StudyEndReason;
import java.time.Instant;
import java.time.LocalDate;

public record StudyReplaySummaryResponse(
        String sessionId, String roomId, String myMemberId, LocalDate studyDate,
        Instant startedAt, Instant endedAt, StudyEndReason endReason, TerritorySummary summary) {}
