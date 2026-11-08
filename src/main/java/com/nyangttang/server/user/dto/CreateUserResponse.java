package com.nyangttang.server.user.dto;

import java.time.Instant;

public record CreateUserResponse(
        Long id,
        String nickname,
        String accessToken,
        Instant expiresAt
) {
}