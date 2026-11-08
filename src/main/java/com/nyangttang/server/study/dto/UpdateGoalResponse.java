package com.nyangttang.server.study.dto;

import java.time.Instant;
import java.time.LocalDate;

public record UpdateGoalResponse(String roomId, String memberId, LocalDate studyDate,
        int dailyGoalSeconds, long todayStudyMs, long remainingGoalSeconds,
        boolean goalAchieved, Instant firstGoalAchievedAt, long snapshotVersion, long lastEventSeq) {}
