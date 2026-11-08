package com.nyangttang.server.device.service;

import com.nyangttang.server.device.dto.PhysicalHeartbeatResponse;
import com.nyangttang.server.device.dto.PhysicalHeartbeatResponse.FaceEvent;
import com.nyangttang.server.study.entity.StudyState;
import java.util.List;

final class PhysicalResponseFactory {
    private PhysicalResponseFactory() {}

    static PhysicalHeartbeatResponse create(StudyState state, List<String> owners,
            String memberId, int goalSeconds, long creditedMs, FaceEvent event) {
        String face = switch (state) {
            case STUDYING -> "STUDY";
            case AWAY_GRACE, AWAY_PAUSED -> "AWAY";
            case RESTING, OFFLINE -> "SLEEP";
        };
        long owned = owners.stream().filter(memberId::equals).count();
        int percent = owners.isEmpty() ? 0 : (int) (owned * 100L / owners.size());
        long remaining = Math.max(0L, goalSeconds * 1000L - creditedMs);
        return new PhysicalHeartbeatResponse(face, percent, (remaining + 59_999L) / 60_000L, event);
    }
}
