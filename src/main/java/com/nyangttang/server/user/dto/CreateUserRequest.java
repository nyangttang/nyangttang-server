package com.nyangttang.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 12, message = "닉네임은 12자 이하여야 합니다.")
        String nickname
) {
    public CreateUserRequest {
        if (nickname != null) {
            nickname = nickname.strip();
        }
    }
}