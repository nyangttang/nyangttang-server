package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.*;

public record MyRoomResponse(
        String roomId, String memberId, String name, String inviteCode,
        RoomMode mode, int stepMs, int awayGraceMs, int slotNo,
        String catName, CatColor catColor, int dailyGoalSeconds, int capacity) {
    public static MyRoomResponse from(RoomMember member) {
        Room room = member.getRoom();
        return new MyRoomResponse(room.getId().toString(), member.getId().toString(),
                room.getName(), room.getInviteCode(), room.getMode(), room.getStepMs(),
                room.getAwayGraceMs(), member.getSlotNo(), member.getCatName(),
                member.getCatColor(), member.getDailyGoalSeconds(), room.getCapacity());
    }
}
