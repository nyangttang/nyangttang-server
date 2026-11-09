package com.nyangttang.server.device.controller;

import com.nyangttang.server.device.dto.*;
import com.nyangttang.server.device.service.PhysicalDeviceService;
import com.nyangttang.server.device.service.DeviceAuthenticationService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@Profile({"dev", "prod"})
public class PhysicalDeviceController {
    private final PhysicalDeviceService service;
    private final AuthService auth;
    private final DeviceAuthenticationService deviceAuth;
    public PhysicalDeviceController(PhysicalDeviceService service, AuthService auth, DeviceAuthenticationService deviceAuth) {
        this.service = service; this.auth = auth;
        this.deviceAuth = deviceAuth;
    }

    @PostMapping("/api/v1/dev/rooms/{roomId}/physical-device")
    public ResponseEntity<Map<String, RegisterPhysicalDeviceResponse>> register(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = DeviceAuthenticationService.HEADER, required = false) String deviceKey,
            @Valid @RequestBody RegisterPhysicalDeviceRequest request) {
        Long userId = auth.getMe(authorization).id();
        deviceAuth.requireValidKey(request.deviceCode(), deviceKey);
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(Map.of("data", service.register(roomId, userId, request)));
        } catch (DataIntegrityViolationException conflict) {
            // Handles simultaneous registration of one device code from different rooms.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "기기 등록 정보가 충돌했습니다.");
        }
    }

    // deviceCode identifies the device; X-Device-Key authenticates it before any DB mutation.
    @PostMapping("/api/devices/{deviceCode}/heartbeat")
    public ResponseEntity<PhysicalHeartbeatResponse> heartbeat(
            @PathVariable("deviceCode") String code,
            @RequestHeader(value = DeviceAuthenticationService.HEADER, required = false) String deviceKey,
            @Valid @RequestBody PhysicalHeartbeatRequest request) {
        deviceAuth.requireValidKey(code, deviceKey);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.heartbeat(code, request));
    }
}
