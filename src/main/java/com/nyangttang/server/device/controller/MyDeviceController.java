package com.nyangttang.server.device.controller;

import com.nyangttang.server.device.dto.MyDeviceResponse;
import com.nyangttang.server.device.service.MyDeviceQueryService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

@RestController
public class MyDeviceController {
    private final MyDeviceQueryService service;
    private final AuthService auth;
    public MyDeviceController(MyDeviceQueryService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @GetMapping("/api/v1/rooms/{roomId}/devices/me")
    public ResponseEntity<Map<String, MyDeviceResponse>> myDevice(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var result = service.findMyDevice(roomId, auth.getMe(authorization).id());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Collections.singletonMap("data", result));
    }
}
