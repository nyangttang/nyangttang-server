package com.nyangttang.server.device.dto;

import jakarta.validation.constraints.*;

public record PhysicalHeartbeatRequest(
        @NotNull Boolean phone,
        @NotNull Boolean seat,
        @NotNull @Min(0) @Max(999) Integer distanceCm) {}
