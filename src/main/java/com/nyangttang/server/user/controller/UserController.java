package com.nyangttang.server.user.controller;

import com.nyangttang.server.user.dto.CreateUserRequest;
import com.nyangttang.server.user.dto.CreateUserResponse;
import com.nyangttang.server.user.dto.MeResponse;
import com.nyangttang.server.user.service.AuthService;
import com.nyangttang.server.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(
            UserService userService,
            AuthService authService
    ) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Map<String, CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        CreateUserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("data", response));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, MeResponse>> getMe(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization
    ) {
        MeResponse response = authService.getMe(authorization);

        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("data", response));
    }
}