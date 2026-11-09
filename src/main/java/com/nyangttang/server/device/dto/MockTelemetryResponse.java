package com.nyangttang.server.device.dto;

import com.nyangttang.server.study.entity.StudyState;
import java.time.Instant;

public record MockTelemetryResponse(
        boolean accepted, String reason, String deviceCode, String linkId,
        long lastSeq, String memberId, StudyState state,
        Instant absentSince, Instant lastSeenAt,
        int awayGraceMs, long offlineTimeoutMs
) {}
