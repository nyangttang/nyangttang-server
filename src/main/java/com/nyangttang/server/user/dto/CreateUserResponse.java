package com.nyangttang.server.user.dto;

import com.nyangttang.server.user.entity.User;

public record CreateUserResponse(
        Long id,
        String nickname
) {
    public static CreateUserResponse from(User user) {
        return new CreateUserResponse(
                user.getId(),
                user.getNickname()
        );
    }
}