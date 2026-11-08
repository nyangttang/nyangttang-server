package com.nyangttang.server.study.service;

import com.nyangttang.server.study.entity.StudyState;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class StudyTimeline {
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public record Slice(LocalDate date, Instant from, Instant to, long creditedMs) {}

    public List<Slice> split(StudyState state, Instant from, Instant now,
                             Instant absentSince, Instant lastSeenAt, long graceMs) {
        if (!now.isAfter(from) || lastSeenAt == null) return List.of();
        Instant end = earlier(now, lastSeenAt.plusMillis(StudyStateMachine.OFFLINE_TIMEOUT_MS));
        if (!end.isAfter(from)) return List.of();
        Instant creditEnd = from;
        if (state == StudyState.STUDYING) creditEnd = end;
        if (state == StudyState.AWAY_GRACE) {
            if (absentSince == null) throw new IllegalStateException("유예 시작 시각 누락");
            creditEnd = earlier(end, absentSince.plusMillis(graceMs));
        }
        List<Slice> result = new ArrayList<>();
        Instant cursor = from;
        while (cursor.isBefore(end)) {
            LocalDate date = date(cursor);
            Instant next = earlier(end, midnightAfter(date));
            // Split at grace expiry as well, so the paused transition is recorded at its boundary.
            if (creditEnd.isAfter(cursor) && creditEnd.isBefore(next)) next = creditEnd;
            long millis = Math.max(0, Duration.between(cursor, earlier(next, creditEnd)).toMillis());
            result.add(new Slice(date, cursor, next, millis));
            cursor = next;
        }
        return result;
    }
    public static LocalDate date(Instant at) { return at.atZone(SEOUL).toLocalDate(); }
    public static Instant midnightAfter(LocalDate date) { return date.plusDays(1).atStartOfDay(SEOUL).toInstant(); }
    private static Instant earlier(Instant a, Instant b) { return a.isBefore(b) ? a : b; }
}
