package com.nyangttang.server.room.controller;

import com.nyangttang.server.room.dto.RoomSnapshotResponse;
import com.nyangttang.server.room.service.RoomSnapshotService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomSnapshotController {

    private final RoomSnapshotService roomSnapshotService;
    private final AuthService authService;

    public RoomSnapshotController(
            RoomSnapshotService roomSnapshotService,
            AuthService authService
    ) {
        this.roomSnapshotService = roomSnapshotService;
        this.authService = authService;
    }

    @GetMapping("/{roomId}/snapshot")
    public ResponseEntity<Map<String, RoomSnapshotResponse>> getSnapshot(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization
    ) {
        Long userId = authService.getMe(authorization).id();

        RoomSnapshotResponse response =
                roomSnapshotService.getSnapshot(roomId, userId);

        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("data", response));
    }
}