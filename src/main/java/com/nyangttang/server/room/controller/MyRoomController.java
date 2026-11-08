package com.nyangttang.server.room.controller;

import com.nyangttang.server.room.dto.MyRoomResponse;
import com.nyangttang.server.room.service.MyRoomQueryService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

@RestController
public class MyRoomController {
    private final MyRoomQueryService service;
    private final AuthService auth;
    public MyRoomController(MyRoomQueryService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @GetMapping("/api/v1/users/me/room")
    public ResponseEntity<Map<String, MyRoomResponse>> myRoom(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var result = service.findMyRoom(auth.getMe(authorization).id());
        // Map.of rejects null: a user with no room must receive {"data":null}.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Collections.singletonMap("data", result));
    }
}
