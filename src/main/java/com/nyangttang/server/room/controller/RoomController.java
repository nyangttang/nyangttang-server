package com.nyangttang.server.room.controller;

import com.nyangttang.server.room.dto.CreateRoomRequest;
import com.nyangttang.server.room.dto.CreateRoomResponse;
import com.nyangttang.server.room.service.RoomService;
import com.nyangttang.server.user.service.AuthService;
import com.nyangttang.server.room.dto.JoinRoomRequest;
import com.nyangttang.server.room.dto.JoinRoomResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final AuthService authService;

    public RoomController(
            RoomService roomService,
            AuthService authService
    ) {
        this.roomService = roomService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Map<String, CreateRoomResponse>> createRoom(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        Long userId = authService.getMe(authorization).id();

        CreateRoomResponse response =
                roomService.createRoom(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("data", response));
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, JoinRoomResponse>> joinRoom(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization,
            @Valid @RequestBody JoinRoomRequest request
    ) {
        Long userId = authService.getMe(authorization).id();

        JoinRoomResponse response =
                roomService.joinRoom(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("data", response));
    }
}