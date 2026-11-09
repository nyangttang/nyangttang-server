package com.nyangttang.server.device.service;

import com.nyangttang.server.device.dto.PhysicalHeartbeatResponse.FaceEvent;
import com.nyangttang.server.device.entity.DeviceFeedbackState;
import com.nyangttang.server.replay.entity.RoomEvent;
import com.nyangttang.server.replay.repository.RoomEventRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Map;

@Service
@Profile("dev")
@Transactional(propagation = Propagation.MANDATORY)
public class DeviceFeedbackService {
    private final RoomEventRepository events;
    public DeviceFeedbackService(RoomEventRepository events) { this.events = events; }

    // Caller holds the room lock. Return one new expression per heartbeat.
    public FaceEvent next(Long roomId, String memberId, DeviceFeedbackState cursor,
                          java.time.LocalDate studyDate, boolean goalAchieved) {
        if (!goalAchieved) cursor.clearGoalEvent();
        return nextFiltered(roomId, memberId, cursor, action -> goalAchieved
                && studyDate.toString().equals(action.get("studyDate")));
    }

    public FaceEvent next(Long roomId, String memberId, DeviceFeedbackState cursor) {
        return nextFiltered(roomId, memberId, cursor, action -> true);
    }

    private FaceEvent nextFiltered(Long roomId, String memberId, DeviceFeedbackState cursor,
                                   java.util.function.Predicate<Map<String, Object>> goalEligible) {
        var frames = events.findByRoom_IdAndSeqGreaterThanEqualOrderBySeqAsc(
                roomId, cursor.getNextRoomSeq(), PageRequest.of(0, 50));
        for (RoomEvent frame : frames) {
            int start = frame.getSeq().equals(cursor.getNextRoomSeq()) ? cursor.getNextActionIndex() : 0;
            var actions = frame.getActions();
            for (int i = start; i < actions.size(); i++) {
                String type = recipientType(actions.get(i), memberId);
                cursor.consumed(frame.getSeq(), i, actions.size());
                if ("GOAL".equals(type) && !goalEligible.test(actions.get(i))) continue;
                if (type != null) return cursor.remember(type);
            }
        }
        // Firmware ignores IDs it has already displayed. No new event => repeat last, or null.
        return cursor.lastEvent();
    }

    static String recipientType(Map<String, Object> action, String memberId) {
        Object type = action.get("type");
        if ("GOAL".equals(type) && memberId.equals(action.get("memberId"))) return "GOAL";
        if ("INVADE".equals(type) && memberId.equals(action.get("targetMemberId"))) return "INVADE";
        if ("HISS".equals(type) && (memberId.equals(action.get("memberId"))
                || memberId.equals(action.get("otherMemberId")))) return "HISS";
        return null;
    }
}
