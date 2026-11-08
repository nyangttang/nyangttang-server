package com.nyangttang.server.user.dto;

import com.nyangttang.server.user.entity.User;

public record MeResponse(
        Long id,
        String nickname
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getNickname()
        );
    }
}