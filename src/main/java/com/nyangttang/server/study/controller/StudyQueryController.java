package com.nyangttang.server.study.controller;

import com.nyangttang.server.study.dto.*;
import com.nyangttang.server.study.service.StudyQueryService;
import com.nyangttang.server.user.service.AuthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me/study")
public class StudyQueryController {
    private final StudyQueryService service;
    private final AuthService auth;
    public StudyQueryController(StudyQueryService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, DailyStudyResponse>> daily(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var response = service.daily(auth.getMe(authorization).id(), from, to);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", response));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, StudySessionListResponse>> sessions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "beforeId", required = false) Long beforeId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        var response = service.sessions(auth.getMe(authorization).id(), date, beforeId, limit);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data", response));
    }
}
