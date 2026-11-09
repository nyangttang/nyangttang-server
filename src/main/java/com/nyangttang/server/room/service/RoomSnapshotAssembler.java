package com.nyangttang.server.room.service;

import com.nyangttang.server.room.dto.RoomSnapshotResponse;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.repository.RoomMemberRepository;
import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.repository.MemberRuntimeRepository;
import com.nyangttang.server.study.entity.DailyStudyStat;
import com.nyangttang.server.study.repository.DailyStudyStatRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

@Component
public class RoomSnapshotAssembler {
    private final RoomMemberRepository members;
    private final MemberRuntimeRepository runtimes;
    private final DailyStudyStatRepository daily;
    public RoomSnapshotAssembler(RoomMemberRepository members, MemberRuntimeRepository runtimes,
                                 DailyStudyStatRepository daily) {
        this.members = members; this.runtimes = runtimes; this.daily = daily;
    }
    public RoomSnapshotResponse build(Room room, RoomRuntime map, String myMemberId, LocalDate date) {
        List<String> owners = new ArrayList<>(map.getOwners());
        if (owners.size() != 100) throw new IllegalStateException("지도는 100칸이어야 합니다.");
        Map<String, Integer> counts = new HashMap<>();
        for (String owner : owners) if (owner != null) counts.merge(owner, 1, Integer::sum);
        Map<Long, MemberRuntime> states = new HashMap<>();
        for (MemberRuntime state : runtimes.findAllByRoomId(room.getId())) states.put(state.getId(), state);
        Map<Long, DailyStudyStat> stats = new HashMap<>();
        for (DailyStudyStat stat : daily.findAllByMember_Room_IdAndStudyDate(room.getId(), date)) {
            stats.put(stat.getMember().getId(), stat);
        }
        List<RoomSnapshotResponse.MemberSnapshot> people = new ArrayList<>();
        for (RoomMember member : members.findAllByRoom_IdOrderBySlotNoAsc(room.getId())) {
            MemberRuntime state = states.get(member.getId());
            if (state == null) throw new IllegalStateException("참가자 초기 상태 누락");
            DailyStudyStat stat = stats.get(member.getId());
            long credited = stat == null ? 0 : stat.getCreditedMs();
            int target = stat == null ? member.getDailyGoalSeconds() : stat.getGoalSeconds();
            long remaining = Math.max(0, target * 1000L - credited);
            people.add(new RoomSnapshotResponse.MemberSnapshot(member.getId().toString(),
                    member.getSlotNo(), member.getCatName(), member.getCatColor(), state.getState(),
                    state.getX(), state.getY(), counts.getOrDefault(member.getId().toString(), 0),
                    target, credited, credited / 1000, (remaining + 999) / 1000,
                    stat != null && stat.isGoalAchieved()));
        }
        return new RoomSnapshotResponse(1, room.getId().toString(), room.getName(), room.getMode(),
                10, 10, map.getSnapshotVersion(), map.getLastEventSeq(), myMemberId, date, owners, people);
    }
    // Replay snapshots are public room state: never include a caller-specific myMemberId or tokens.
    public Map<String, Object> publicSnapshot(Room room, RoomRuntime map, LocalDate date) {
        RoomSnapshotResponse response = build(room, map, null, date);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", response.schemaVersion()); result.put("roomId", response.roomId());
        result.put("name", response.name()); result.put("mode", response.mode().name());
        result.put("width", 10); result.put("height", 10);
        result.put("snapshotVersion", response.snapshotVersion()); result.put("lastEventSeq", response.lastEventSeq());
        result.put("studyDate", date.toString()); result.put("owners", response.owners());
        List<Map<String, Object>> people = new ArrayList<>();
        for (var member : response.members()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberId", member.memberId()); row.put("slotNo", member.slotNo());
            row.put("catName", member.catName()); row.put("catColor", member.catColor().name());
            row.put("state", member.state().name()); row.put("x", member.x()); row.put("y", member.y());
            row.put("ownedCellCount", member.ownedCellCount()); row.put("dailyGoalSeconds", member.dailyGoalSeconds());
            row.put("todayStudyMs", member.todayStudyMs()); row.put("todayStudySeconds", member.todayStudySeconds());
            row.put("remainingGoalSeconds", member.remainingGoalSeconds()); row.put("goalAchieved", member.goalAchieved());
            people.add(row);
        }
        result.put("members", people);
        return result;
    }
}
