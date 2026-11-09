package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import com.nyangttang.server.study.entity.StudyState;
import java.time.LocalDate;
import java.util.List;

public record RoomRankingResponse(
        String roomId, String myMemberId, LocalDate studyDate,
        long snapshotVersion, long lastEventSeq, Sort sort, List<Item> items
) {
    public enum Sort { LAND, STUDY }
    public record Item(
            int rank, int landRank, int studyRank, String memberId, boolean me,
            String catName, CatColor catColor, StudyState state,
            int ownedCellCount, long todayStudyMs, long todayStudySeconds,
            int dailyGoalSeconds, long remainingGoalSeconds, boolean goalAchieved
    ) {}
}
