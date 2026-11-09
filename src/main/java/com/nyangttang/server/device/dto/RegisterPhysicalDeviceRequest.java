package com.nyangttang.server.device.dto;

import jakarta.validation.constraints.*;

public record RegisterPhysicalDeviceRequest(
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "[A-Z0-9]+(?:-[A-Z0-9]+)*") String deviceCode) {}
