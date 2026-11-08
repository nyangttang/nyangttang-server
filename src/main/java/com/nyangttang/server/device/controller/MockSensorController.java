package com.nyangttang.server.device.controller;

import com.nyangttang.server.device.dto.*;
import com.nyangttang.server.device.service.MockSensorService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/dev/rooms/{roomId}/mock-device")
public class MockSensorController {
    private final MockSensorService service;
    private final AuthService auth;
    public MockSensorController(MockSensorService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @PostMapping("/connect")
    public ResponseEntity<Map<String, MockTelemetryResponse>> connect(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ok(service.connect(roomId, auth.getMe(authorization).id()));
    }

    @PostMapping("/telemetry")
    public ResponseEntity<Map<String, MockTelemetryResponse>> telemetry(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MockTelemetryRequest request) {
        return ok(service.receive(roomId, auth.getMe(authorization).id(), request));
    }

    private ResponseEntity<Map<String, MockTelemetryResponse>> ok(MockTelemetryResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", response));
    }
}
