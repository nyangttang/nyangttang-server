package com.nyangttang.server.study.dto;

import com.nyangttang.server.study.entity.StudyEndReason;
import com.nyangttang.server.study.repository.StudySessionSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudySessionListResponse(List<Item> items, String nextBeforeId, boolean hasMore) {
    public record Item(String sessionId, String roomId, LocalDate studyDate,
                       Instant startedAt, Instant endedAt, StudyEndReason endReason,
                       long creditedMs, boolean replayReady) {
        public static Item from(StudySessionSummary row) {
            return new Item(row.getId().toString(), row.getRoomId().toString(), row.getStudyDate(),
                    row.getStartedAt(), row.getEndedAt(), row.getEndReason(), row.getCreditedMs(),
                    row.getEndedAt() != null);
        }
    }
}
