package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import com.nyangttang.server.room.entity.RoomMode;
import com.nyangttang.server.study.entity.StudyState;

public record CreateRoomResponse(
        Long roomId,
        String name,
        String inviteCode,
        RoomMode mode,
        Integer stepMs,
        Integer awayGraceMs,
        Long memberId,
        String catName,
        CatColor catColor,
        Integer dailyGoalSeconds,
        StudyState state,
        int x,
        int y,
        int capacity
) {
}