package com.nyangttang.server.study.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdateGoalRequest(
        @NotNull LocalDate studyDate,
        @NotNull @Min(30) @Max(43200) Integer dailyGoalSeconds) {}
