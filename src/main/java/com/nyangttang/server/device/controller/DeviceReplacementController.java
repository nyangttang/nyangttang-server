package com.nyangttang.server.device.controller;
import com.nyangttang.server.device.dto.RegisterPhysicalDeviceRequest;
import com.nyangttang.server.device.service.PhysicalDeviceService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @Profile("dev")
public class DeviceReplacementController {
    private final PhysicalDeviceService service;private final AuthService auth;
    public DeviceReplacementController(PhysicalDeviceService service,AuthService auth){this.service=service;this.auth=auth;}
    public record ReplaceRequest(@NotBlank @Size(max=32) String expectedDeviceCode,
            @NotBlank @Size(min=3,max=32) @Pattern(regexp="[A-Z0-9]+(?:-[A-Z0-9]+)*") String deviceCode){}
    @PostMapping("/api/v1/dev/rooms/{roomId}/physical-device/replace")
    public ResponseEntity<?> replace(@PathVariable("roomId") Long roomId,
            @RequestHeader(value="Authorization",required=false) String authorization,@Valid @RequestBody ReplaceRequest request){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",service.replace(roomId,auth.getMe(authorization).id(),
                new RegisterPhysicalDeviceRequest(request.deviceCode()),request.expectedDeviceCode())));
    }
}
