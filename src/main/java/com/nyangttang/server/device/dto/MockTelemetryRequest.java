package com.nyangttang.server.device.dto;

import jakarta.validation.constraints.*;

public record MockTelemetryRequest(
        @NotBlank @Pattern(regexp = "[a-f0-9]{8}") String linkId,
        @NotNull @Min(0) @Max(4_294_967_295L) Long seq,
        @NotNull Boolean phonePresent,
        @NotNull Boolean personPresent
) {}
