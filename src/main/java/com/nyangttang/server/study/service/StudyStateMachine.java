package com.nyangttang.server.study.service;

import com.nyangttang.server.study.entity.StudyState;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class StudyStateMachine {
    public static final long OFFLINE_TIMEOUT_MS = 10_000;

    public record Decision(StudyState state, Instant absentSince) {}

    // Called before accepting fresh input, and by the timer without new input.
    public Decision tick(StudyState state, Instant absentSince,
                         Instant lastSeenAt, Instant now, long graceMs) {
        if (lastSeenAt == null || !now.isBefore(lastSeenAt.plusMillis(OFFLINE_TIMEOUT_MS))) {
            return new Decision(StudyState.OFFLINE, null);
        }
        if (state == StudyState.AWAY_GRACE && absentSince != null
                && !now.isBefore(absentSince.plusMillis(graceMs))) {
            return new Decision(StudyState.AWAY_PAUSED, absentSince);
        }
        return new Decision(state, absentSince);
    }

    // State here must first be advanced using tick().
    public Decision receive(StudyState state, Instant absentSince,
                            boolean phone, boolean person, Instant now) {
        if (!phone) return new Decision(StudyState.RESTING, null);
        if (person) return new Decision(StudyState.STUDYING, null);
        if (state == StudyState.STUDYING) {
            return new Decision(StudyState.AWAY_GRACE, now);
        }
        if (state == StudyState.AWAY_GRACE) {
            return new Decision(state, absentSince);
        }
        return new Decision(StudyState.AWAY_PAUSED,
                absentSince == null ? now : absentSince);
    }
}
