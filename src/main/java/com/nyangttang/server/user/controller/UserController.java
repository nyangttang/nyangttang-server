package com.nyangttang.server.user.controller;

import com.nyangttang.server.user.dto.CreateUserRequest;
import com.nyangttang.server.user.dto.CreateUserResponse;
import com.nyangttang.server.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Map<String, CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        CreateUserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("data", response));
    }
}