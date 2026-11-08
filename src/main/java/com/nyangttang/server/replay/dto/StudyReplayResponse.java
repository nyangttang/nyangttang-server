package com.nyangttang.server.replay.dto;

import com.nyangttang.server.replay.entity.RoomEvent;
import com.nyangttang.server.study.entity.StudyEndReason;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StudyReplayResponse(
        String sessionId, String roomId, String myMemberId, LocalDate studyDate,
        Instant startedAt, Instant endedAt, StudyEndReason endReason,
        long startEventSeq, long endEventSeq,
        Map<String, Object> startSnapshot, Map<String, Object> endSnapshot,
        List<Frame> frames, Long nextAfterSeq, boolean hasMore, TerritorySummary summary) {
    public record Frame(long seq, Instant occurredAt, long snapshotVersion,
                        Map<String, Object> snapshot, List<Map<String, Object>> actions) {
        public static Frame from(RoomEvent event) {
            return new Frame(event.getSeq(), event.getOccurredAt(), event.getSnapshotVersion(),
                    event.getSnapshot(), event.getActions());
        }
    }
}
