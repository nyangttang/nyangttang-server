package com.nyangttang.server.notification.service;

import com.nyangttang.server.notification.dto.*;
import com.nyangttang.server.notification.repository.RoomNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class NotificationService {
    private final RoomNotificationRepository notifications;
    public NotificationService(RoomNotificationRepository notifications) { this.notifications = notifications; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public NotificationListResponse list(Long userId, Long beforeId, int limit, boolean unreadOnly) {
        if (limit < 1 || limit > 50 || (beforeId != null && beforeId < 1))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit은 1~50, beforeId는 양수여야 합니다.");
        var rows = notifications.findPage(userId, beforeId, unreadOnly, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        var items = rows.stream().limit(limit).map(NotificationResponse::from).toList();
        return new NotificationListResponse(items, hasMore ? items.getLast().id() : null, hasMore,
                notifications.countByRecipient_User_IdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        if (id == null || id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알림 ID는 양수여야 합니다.");
        var notification = notifications.findOwnedForUpdate(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markRead(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        return NotificationResponse.from(notification);
    }
}
