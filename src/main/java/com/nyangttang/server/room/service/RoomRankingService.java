package com.nyangttang.server.room.service;

import com.nyangttang.server.room.dto.RoomRankingResponse;
import com.nyangttang.server.room.dto.RoomRankingResponse.Sort;
import com.nyangttang.server.room.dto.RoomSnapshotResponse.MemberSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

@Service
public class RoomRankingService {
    private final RoomSnapshotService snapshots;

    public RoomRankingService(RoomSnapshotService snapshots) { this.snapshots = snapshots; }

    public RoomRankingResponse getRanking(Long roomId, Long userId, Sort sort) {
        if (roomId == null || roomId < 1 || sort == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방 번호와 정렬 기준을 확인하세요.");
        }
        // Reuses room membership checks and one consistent, read-only snapshot transaction.
        var snapshot = snapshots.getSnapshot(roomId, userId);
        var people = snapshot.members();
        ToLongFunction<MemberSnapshot> score = sort == Sort.LAND
                ? MemberSnapshot::ownedCellCount : MemberSnapshot::todayStudyMs;
        var ordered = people.stream().sorted(Comparator.comparingLong(score).reversed()
                .thenComparingInt(MemberSnapshot::slotNo).thenComparing(MemberSnapshot::memberId)).toList();
        var items = ordered.stream().map(m -> {
            int landRank = rank(people, m.ownedCellCount(), MemberSnapshot::ownedCellCount);
            int studyRank = rank(people, m.todayStudyMs(), MemberSnapshot::todayStudyMs);
            return new RoomRankingResponse.Item(sort == Sort.LAND ? landRank : studyRank,
                    landRank, studyRank, m.memberId(), m.memberId().equals(snapshot.myMemberId()),
                    m.catName(), m.catColor(), m.state(), m.ownedCellCount(), m.todayStudyMs(),
                    m.todayStudySeconds(), m.dailyGoalSeconds(), m.remainingGoalSeconds(), m.goalAchieved());
        }).toList();
        return new RoomRankingResponse(snapshot.roomId(), snapshot.myMemberId(), snapshot.studyDate(),
                snapshot.snapshotVersion(), snapshot.lastEventSeq(), sort, items);
    }

    // Competition ranking: 1, 1, 3. Slot order stabilizes display without breaking a tie.
    private static int rank(List<MemberSnapshot> people, long value, ToLongFunction<MemberSnapshot> score) {
        return 1 + (int) people.stream().filter(p -> score.applyAsLong(p) > value).count();
    }
}
