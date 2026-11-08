package com.nyangttang.server.game.service;

import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.repository.MemberRuntimeRepository;
import com.nyangttang.server.room.entity.Room;
import com.nyangttang.server.study.entity.StudyState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

// Caller must hold the room write lock used by telemetry and the scheduler.
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CatMovementService {
    private final MemberRuntimeRepository runtimes;
    private final CatPathPolicy path;
    public CatMovementService(MemberRuntimeRepository runtimes, CatPathPolicy path) {
        this.runtimes = runtimes; this.path = path;
    }

    public List<Map<String, Object>> applyCredit(Room room, RoomRuntime map,
                                                 MemberRuntime cat, long credit, Instant effectiveAt) {
        if (credit == 0) return List.of();
        if (cat.getState() != StudyState.STUDYING && cat.getState() != StudyState.AWAY_GRACE) {
            throw new IllegalStateException("공부 인정 상태에서만 이동 시간을 추가할 수 있습니다.");
        }
        if (credit < 0 || credit > room.getStepMs() - cat.getStepRemainderMs()) {
            throw new IllegalArgumentException("먼저 이동 경계로 시간 구간을 나눠야 합니다.");
        }
        int steps = cat.earnMovementSteps(credit, room.getStepMs());
        if (steps == 0) return List.of();

        CatPathPolicy.Cell next = path.choose(cat);
        if (next.x() < 0 || next.x() > 9 || next.y() < 0 || next.y() > 9
                || Math.abs(next.x() - cat.getX()) + Math.abs(next.y() - cat.getY()) != 1) {
            throw new IllegalStateException("이동 경로가 유효하지 않습니다.");
        }
        Optional<MemberRuntime> other = runtimes.findAllByRoomId(room.getId()).stream()
                .filter(candidate -> !candidate.getId().equals(cat.getId()))
                .filter(candidate -> candidate.getX() == next.x() && candidate.getY() == next.y())
                .findFirst();
        if (other.isPresent()) {
            cat.allowBacktrackAfterEncounter();
            Map<String, Object> hiss = action("HISS", cat, effectiveAt);
            hiss.put("otherMemberId", other.get().getId().toString());
            hiss.put("targetX", next.x()); hiss.put("targetY", next.y());
            // The step is consumed, but position and territory remain unchanged.
            return List.of(hiss);
        }

        int fromX = cat.getX(), fromY = cat.getY();
        cat.moveTo(next.x(), next.y());
        Map<String, Object> move = action("MOVE", cat, effectiveAt);
        move.put("fromX", fromX); move.put("fromY", fromY);
        move.put("x", next.x()); move.put("y", next.y());
        List<Map<String, Object>> actions = new ArrayList<>(); actions.add(move);

        RoomRuntime.TerritoryChange change = map.visitCell(next.x(), next.y(), cat.getId().toString());
        if (!change.type().equals("NONE")) {
            Map<String, Object> territory = action(change.type(), cat, effectiveAt);
            territory.put("cellIndex", change.cellIndex());
            territory.put("previousOwnerId", change.previousOwnerId());
            territory.put("ownerId", change.ownerId());
            if (change.type().equals("INVADE")) territory.put("targetMemberId", change.previousOwnerId());
            actions.add(territory);
        }
        return actions;
    }
    private Map<String, Object> action(String type, MemberRuntime cat, Instant at) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", type); action.put("memberId", cat.getId().toString());
        action.put("effectiveAt", at.toString()); return action;
    }
}
