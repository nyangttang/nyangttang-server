package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import com.nyangttang.server.room.entity.RoomMode;
import com.nyangttang.server.study.entity.StudyState;

public record JoinRoomResponse(
        Long roomId,
        String name,
        RoomMode mode,
        Long memberId,
        int slotNo,
        String catName,
        CatColor catColor,
        Integer dailyGoalSeconds,
        StudyState state,
        int x,
        int y
) {
}