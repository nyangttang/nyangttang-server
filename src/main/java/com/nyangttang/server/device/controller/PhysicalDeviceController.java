package com.nyangttang.server.device.controller;

import com.nyangttang.server.device.dto.*;
import com.nyangttang.server.device.service.PhysicalDeviceService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@Profile("dev")
public class PhysicalDeviceController {
    private final PhysicalDeviceService service;
    private final AuthService auth;
    public PhysicalDeviceController(PhysicalDeviceService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @PostMapping("/api/v1/dev/rooms/{roomId}/physical-device")
    public ResponseEntity<Map<String, RegisterPhysicalDeviceResponse>> register(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody RegisterPhysicalDeviceRequest request) {
        Long userId = auth.getMe(authorization).id();
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(Map.of("data", service.register(roomId, userId, request)));
        } catch (DataIntegrityViolationException conflict) {
            // Handles simultaneous registration of one device code from different rooms.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "기기 등록 정보가 충돌했습니다.");
        }
    }

    // Legacy local demo protocol: deviceCode is identification, not authentication.
    @PostMapping("/api/devices/{deviceCode}/heartbeat")
    public ResponseEntity<PhysicalHeartbeatResponse> heartbeat(
            @PathVariable("deviceCode") String code, @Valid @RequestBody PhysicalHeartbeatRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.heartbeat(code, request));
    }
}
