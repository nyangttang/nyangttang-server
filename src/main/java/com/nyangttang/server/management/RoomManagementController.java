package com.nyangttang.server.management;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/rooms/{roomId}")
public class RoomManagementController {
    private final RoomManagementService service;private final AuthService auth;
    public RoomManagementController(RoomManagementService service,AuthService auth){this.service=service;this.auth=auth;}
    public record UnlinkRequest(@NotBlank @Size(max=32) String expectedDeviceCode){}
    public record LeaveRequest(@NotNull @Positive Long memberId){}
    @PostMapping("/device/unlink")
    public ResponseEntity<?> unlink(@PathVariable("roomId") Long roomId,
            @RequestHeader(value="Authorization",required=false) String authorization,@Valid @RequestBody UnlinkRequest request){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",service.unlink(roomId,auth.getMe(authorization).id(),request.expectedDeviceCode())));
    }
    @PostMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable("roomId") Long roomId,
            @RequestHeader(value="Authorization",required=false) String authorization,@Valid @RequestBody LeaveRequest request){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",service.leave(roomId,auth.getMe(authorization).id(),request.memberId())));
    }
}
