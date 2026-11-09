package com.nyangttang.server.room.controller;

import com.nyangttang.server.room.dto.RoomRankingResponse;
import com.nyangttang.server.room.dto.RoomRankingResponse.Sort;
import com.nyangttang.server.room.service.RoomRankingService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomRankingController {
    private final RoomRankingService service;
    private final AuthService auth;

    public RoomRankingController(RoomRankingService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @GetMapping("/{roomId}/ranking")
    public ResponseEntity<Map<String, RoomRankingResponse>> ranking(
            @PathVariable("roomId") Long roomId,
            @RequestParam(name = "sort", defaultValue = "LAND") Sort sort,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Long userId = auth.getMe(authorization).id();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("data", service.getRanking(roomId, userId, sort)));
    }
}
