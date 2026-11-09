package com.nyangttang.server.study.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyStudyResponse(LocalDate from, LocalDate to, String timezone,
                                 long totalStudyMs, List<Day> days) {
    public record Day(LocalDate date, long studyMs, long studySeconds,
                      Integer goalSeconds, boolean goalAchieved) {}
}
