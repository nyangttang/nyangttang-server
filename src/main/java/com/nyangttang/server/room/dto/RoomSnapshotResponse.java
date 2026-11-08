package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import com.nyangttang.server.room.entity.RoomMode;
import com.nyangttang.server.study.entity.StudyState;
import java.time.LocalDate;
import java.util.List;

public record RoomSnapshotResponse(
        int schemaVersion, String roomId, String name, RoomMode mode,
        int width, int height, long snapshotVersion, long lastEventSeq,
        String myMemberId, LocalDate studyDate, List<String> owners, List<MemberSnapshot> members
) {
    public record MemberSnapshot(
            String memberId, int slotNo, String catName, CatColor catColor,
            StudyState state, int x, int y, int ownedCellCount, int dailyGoalSeconds,
            long todayStudyMs, long todayStudySeconds, long remainingGoalSeconds, boolean goalAchieved
    ) {}
}
