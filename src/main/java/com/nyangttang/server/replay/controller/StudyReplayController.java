package com.nyangttang.server.replay.controller;

import com.nyangttang.server.replay.dto.*;
import com.nyangttang.server.replay.service.StudyReplayService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/study/sessions/{sessionId}")
public class StudyReplayController {
    private final StudyReplayService service;
    private final AuthService auth;
    public StudyReplayController(StudyReplayService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @GetMapping("/replay")
    public ResponseEntity<Map<String, StudyReplayResponse>> replay(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "afterSeq", required = false) Long afterSeq,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        var result = service.replay(auth.getMe(authorization).id(), sessionId, afterSeq, limit);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", result));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, StudyReplaySummaryResponse>> summary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("sessionId") Long sessionId) {
        var result = service.summary(auth.getMe(authorization).id(), sessionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", result));
    }
}
