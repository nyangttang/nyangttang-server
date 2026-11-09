package com.nyangttang.server.notification.dto;
import java.util.List;
public record NotificationListResponse(
        List<NotificationResponse> items, String nextBeforeId, boolean hasMore, long unreadCount
) {}
