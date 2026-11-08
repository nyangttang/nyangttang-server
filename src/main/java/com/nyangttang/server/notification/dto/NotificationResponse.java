package com.nyangttang.server.notification.dto;

import com.nyangttang.server.notification.entity.*;
import java.time.Instant;

public record NotificationResponse(
        String id, NotificationType type, String roomId, String memberId,
        long eventSeq, int actionIndex, String actorMemberId, Integer cellIndex,
        Instant effectiveAt, Instant createdAt, Instant readAt, String title, String message
) {
    public static NotificationResponse from(RoomNotification n) {
        boolean away = n.getType() == NotificationType.AWAY_TOO_LONG;
        return new NotificationResponse(n.getId().toString(), n.getType(), n.getRoom().getId().toString(),
                n.getRecipient().getId().toString(), n.getEventSeq(), n.getActionIndex(),
                n.getActorMemberId() == null ? null : n.getActorMemberId().toString(), n.getCellIndex(),
                n.getEffectiveAt(), n.getCreatedAt(), n.getReadAt(),
                away ? "자리를 오래 비웠어요" : "내 땅을 빼앗겼어요",
                away ? "자리 비움 유예 시간이 지나 공부 시간 누적과 고양이 이동이 멈췄어요."
                     : "쉬는 동안 친구 고양이가 내 땅 한 칸을 가져갔어요.");
    }
}
