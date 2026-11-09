package com.nyangttang.server.notification.controller;

import com.nyangttang.server.notification.dto.*;
import com.nyangttang.server.notification.service.NotificationService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me/notifications")
public class NotificationController {
    private final NotificationService service;
    private final AuthService auth;
    public NotificationController(NotificationService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }
    @GetMapping
    public ResponseEntity<Map<String, NotificationListResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",
                service.list(auth.getMe(authorization).id(), beforeId, limit, unreadOnly)));
    }
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, NotificationResponse>> read(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("notificationId") Long notificationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",
                service.markRead(auth.getMe(authorization).id(), notificationId)));
    }
}
