package com.nyangttang.server.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PushInput(@NotBlank @Size(max=2048) String endpoint, @NotNull @Valid Keys keys) {
    public record Keys(@NotBlank @Size(max=128) String p256dh, @NotBlank @Size(max=32) String auth) {}
}
