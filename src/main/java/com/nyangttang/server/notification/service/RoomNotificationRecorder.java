package com.nyangttang.server.notification.service;

import com.nyangttang.server.notification.entity.*;
import com.nyangttang.server.notification.repository.RoomNotificationRepository;
import com.nyangttang.server.replay.entity.RoomEvent;
import com.nyangttang.server.room.repository.RoomMemberRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

@Component
public class RoomNotificationRecorder {
    private final org.springframework.context.ApplicationEventPublisher publisher;
    private final RoomNotificationRepository notifications;
    private final RoomMemberRepository members;
    public RoomNotificationRecorder(RoomNotificationRepository notifications, RoomMemberRepository members, org.springframework.context.ApplicationEventPublisher publisher) {
        this.notifications = notifications; this.members = members; this.publisher = publisher;
    }

    // Synchronous listener: the event and its notification commit or roll back together.
    // The publisher holds the room write lock. Do not make this listener asynchronous.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRoomEvent(RoomEvent event) {
        for (int index = 0; index < event.getActions().size(); index++) {
            var action = event.getActions().get(index);
            NotificationType type;
            Long recipientId, actorId = null;
            Integer cellIndex = null;
            if ("STATE_CHANGED".equals(action.get("type"))
                    && "AWAY_GRACE".equals(action.get("from")) && "AWAY_PAUSED".equals(action.get("to"))) {
                type = NotificationType.AWAY_TOO_LONG;
                recipientId = positiveId(action.get("memberId"));
            } else if ("INVADE".equals(action.get("type"))) {
                recipientId = positiveId(action.get("targetMemberId"));
                // Use the saved event snapshot, not the user's later state at polling time.
                if (!"RESTING".equals(stateAtEvent(event, recipientId))) continue;
                type = NotificationType.LAND_LOST;
                actorId = positiveId(action.get("memberId"));
                cellIndex = Integer.valueOf(Objects.requireNonNull(action.get("cellIndex")).toString());
            } else continue;
            if (notifications.existsByRoom_IdAndEventSeqAndActionIndex(event.getRoom().getId(), event.getSeq(), index)) continue;
            var recipient = members.findById(recipientId).orElseThrow(() ->
                    new IllegalStateException("알림 수신 참가자 데이터 누락"));
            Instant effectiveAt = Instant.parse(Objects.requireNonNull(action.get("effectiveAt")).toString());
            var saved = notifications.save(new RoomNotification(event.getRoom(), recipient, event.getSeq(), index,
                    type, actorId, cellIndex, effectiveAt, event.getOccurredAt()));
            publisher.publishEvent(saved);
        }
    }
    private static Long positiveId(Object raw) {
        long id = Long.parseLong(Objects.requireNonNull(raw).toString());
        if (id < 1) throw new IllegalArgumentException("참가자 ID는 양수여야 합니다.");
        return id;
    }
    private static String stateAtEvent(RoomEvent event, Long memberId) {
        if (event.getSnapshot().get("members") instanceof List<?> people) {
            for (Object item : people) {
                if (item instanceof Map<?, ?> row && memberId.toString().equals(String.valueOf(row.get("memberId"))))
                    return Objects.requireNonNull(row.get("state")).toString();
            }
        }
        throw new IllegalStateException("점령 이벤트에 피해 참가자의 상태가 없습니다.");
    }
}
