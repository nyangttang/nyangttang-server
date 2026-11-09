package com.nyangttang.server.study.controller;

import com.nyangttang.server.study.dto.*;
import com.nyangttang.server.study.service.StudyGoalService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class StudyGoalController {
    private final StudyGoalService goals;
    private final AuthService auth;
    public StudyGoalController(StudyGoalService goals, AuthService auth) { this.goals = goals; this.auth = auth; }

    @PostMapping("/{roomId}/goal")
    public ResponseEntity<Map<String, UpdateGoalResponse>> update(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateGoalRequest request) {
        Long userId = auth.getMe(authorization).id();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("data", goals.update(roomId, userId, request)));
    }
}
