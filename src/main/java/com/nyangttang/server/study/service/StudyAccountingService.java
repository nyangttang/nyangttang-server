package com.nyangttang.server.study.service;

import com.nyangttang.server.device.entity.Device;
import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.service.CatMovementService;
import com.nyangttang.server.game.service.MovementTimeline;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.service.RoomSnapshotAssembler;
import com.nyangttang.server.study.entity.*;
import com.nyangttang.server.study.repository.*;
import com.nyangttang.server.replay.entity.RoomEvent;
import com.nyangttang.server.replay.repository.RoomEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

// Call only inside a transaction holding the room's write lock.
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StudyAccountingService {
    private final ApplicationEventPublisher publisher;
    private final MovementTimeline movementTimeline;
    private final CatMovementService movement;
    private final StudyTimeline timeline;
    private final StudyStateMachine machine;
    private final StudySessionRepository sessions;
    private final DailyStudyStatRepository daily;
    private final RoomEventRepository events;
    private final RoomSnapshotAssembler snapshots;

    public StudyAccountingService(StudyTimeline timeline, StudyStateMachine machine,
                                  StudySessionRepository sessions, DailyStudyStatRepository daily,
                                  RoomEventRepository events, RoomSnapshotAssembler snapshots,
                                  MovementTimeline movementTimeline, CatMovementService movement,
                                  ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        this.timeline = timeline; this.machine = machine; this.sessions = sessions;
        this.daily = daily; this.events = events; this.snapshots = snapshots;
        this.movementTimeline = movementTimeline; this.movement = movement;
    }

    public void advance(Room room, RoomRuntime map, MemberRuntime runtime, Device device, Instant requestedNow) {
        Instant now = later(requestedNow, runtime.getLastAccountedAt());
        StudySession open = open(runtime);
        List<StudyTimeline.Slice> slices = timeline.split(runtime.getState(), runtime.getLastAccountedAt(),
                now, runtime.getAbsentSince(), device.getLastSeenAt(), room.getAwayGraceMs());
        slices = movementTimeline.split(slices, runtime.getStepRemainderMs(), room.getStepMs());

        for (var slice : slices) {
            List<Map<String, Object>> actions = new ArrayList<>();
            if (slice.creditedMs() > 0) {
                if (open == null || !open.getStudyDate().equals(slice.date())) {
                    throw new IllegalStateException("인정 시간을 저장할 공부 구간이 없습니다.");
                }
                DailyStudyStat stat = stat(runtime.getMember(), slice.date());
                open.addCredit(slice.creditedMs());
                Instant goalAt = stat.addCredit(slice.creditedMs(), slice.from());
                if (goalAt != null) {
                    Map<String, Object> goal = action("GOAL", runtime, goalAt);
                    goal.put("studyDate", slice.date().toString()); actions.add(goal);
                }
                actions.addAll(movement.applyCredit(room, map, runtime, slice.creditedMs(), slice.to()));
            }
            runtime.accountThrough(slice.to());
            change(runtime, machine.tick(runtime.getState(), runtime.getAbsentSince(),
                    device.getLastSeenAt(), slice.to(), room.getAwayGraceMs()), slice.to(), actions);

            boolean offline = runtime.getState() == StudyState.OFFLINE;
            boolean boundary = slice.to().equals(StudyTimeline.midnightAfter(slice.date()));
            boolean close = open != null && (offline || boundary);
            StudyEndReason reason = offline ? StudyEndReason.OFFLINE : StudyEndReason.DAY_BOUNDARY;
            if (close) actions.add(endAction(runtime, slice.to(), reason));

            Frame frame = null;
            if (!actions.isEmpty()) frame = record(room, map, now, StudyTimeline.date(slice.to()), actions);
            else if (slice.creditedMs() > 0) map.markStateChanged(now);

            if (close) {
                finish(open, slice.to(), reason, frame);
                open = null;
                if (!offline) open = start(room, map, runtime, slice.to(), now);
            }
        }

        // Covers empty intervals (e.g. no heartbeat) and time after the heartbeat deadline.
        List<Map<String, Object>> actions = new ArrayList<>();
        Instant effectiveAt = device.getLastSeenAt() == null ? now :
                earlier(now, device.getLastSeenAt().plusMillis(StudyStateMachine.OFFLINE_TIMEOUT_MS));
        change(runtime, machine.tick(runtime.getState(), runtime.getAbsentSince(),
                device.getLastSeenAt(), now, room.getAwayGraceMs()), effectiveAt, actions);
        if (open != null && runtime.getState() == StudyState.OFFLINE) {
            Instant end = later(open.getStartedAt(), effectiveAt);
            actions.add(endAction(runtime, end, StudyEndReason.OFFLINE));
            Frame frame = record(room, map, now, StudyTimeline.date(end), actions);
            finish(open, end, StudyEndReason.OFFLINE, frame);
        } else if (!actions.isEmpty()) record(room, map, now, StudyTimeline.date(now), actions);
        runtime.accountThrough(now);
    }

    // Caller has already accounted the old state before storing this fresh input.
    public void afterInput(Room room, RoomRuntime map, MemberRuntime runtime,
                           boolean phone, boolean person, Instant requestedNow) {
        Instant now = later(requestedNow, runtime.getLastAccountedAt());
        List<Map<String, Object>> actions = new ArrayList<>();
        change(runtime, machine.receive(runtime.getState(), runtime.getAbsentSince(), phone, person, now), now, actions);
        StudySession open = open(runtime);
        if (runtime.getState() == StudyState.RESTING && open != null) {
            actions.add(endAction(runtime, now, StudyEndReason.PHONE_REMOVED));
            Frame frame = record(room, map, now, StudyTimeline.date(now), actions);
            finish(open, now, StudyEndReason.PHONE_REMOVED, frame);
        } else if (runtime.getState() == StudyState.STUDYING && open == null) {
            // Start frame also includes the transition into studying.
            start(room, map, runtime, now, now, actions);
        } else if (!actions.isEmpty()) record(room, map, now, StudyTimeline.date(now), actions);
        runtime.accountThrough(now);
    }

    public void disconnect(Room room, RoomRuntime map, MemberRuntime runtime,
                           Instant requestedNow, boolean serverRestart) {
        Instant now = later(requestedNow, runtime.getLastAccountedAt());
        // Restart must NOT account any interval since the last persisted checkpoint.
        Instant end = serverRestart ? runtime.getLastAccountedAt() : now;
        StudyEndReason reason = serverRestart ? StudyEndReason.SERVER_RESTART : StudyEndReason.OFFLINE;
        List<Map<String, Object>> actions = new ArrayList<>();
        change(runtime, new StudyStateMachine.Decision(StudyState.OFFLINE, null), end, actions);
        StudySession open = open(runtime);
        if (open != null) {
            end = later(end, open.getStartedAt());
            actions.add(endAction(runtime, end, reason));
            Frame frame = record(room, map, now, StudyTimeline.date(end), actions);
            finish(open, end, reason, frame);
        } else if (!actions.isEmpty()) record(room, map, now, StudyTimeline.date(now), actions);
        runtime.accountThrough(now);
    }

    private StudySession start(Room room, RoomRuntime map, MemberRuntime runtime, Instant at, Instant recordedAt) {
        return start(room, map, runtime, at, recordedAt, new ArrayList<>());
    }
    private StudySession start(Room room, RoomRuntime map, MemberRuntime runtime,
                               Instant at, Instant recordedAt, List<Map<String, Object>> actions) {
        stat(runtime.getMember(), StudyTimeline.date(at));
        actions.add(action("STUDY_STARTED", runtime, at));
        Frame frame = record(room, map, recordedAt, StudyTimeline.date(at), actions);
        return sessions.save(new StudySession(runtime.getMember(), StudyTimeline.date(at), at,
                frame.seq(), frame.snapshot()));
    }
    private void finish(StudySession open, Instant at, StudyEndReason reason, Frame frame) {
        open.finish(at, reason, frame.seq(), frame.snapshot());
        // Flush the generated open_member_id to NULL before inserting the next day's open session.
        sessions.flush();
    }
    private StudySession open(MemberRuntime runtime) {
        return sessions.findByMember_IdAndEndedAtIsNull(runtime.getId()).orElse(null);
    }
    private DailyStudyStat stat(RoomMember member, LocalDate date) {
        return daily.findByMember_IdAndStudyDate(member.getId(), date).orElseGet(() ->
                daily.save(new DailyStudyStat(member, date, member.getDailyGoalSeconds())));
    }
    private void change(MemberRuntime runtime, StudyStateMachine.Decision decision,
                        Instant at, List<Map<String, Object>> actions) {
        StudyState before = runtime.getState();
        if (runtime.changeStudyState(decision.state(), decision.absentSince())) {
            Map<String, Object> action = action("STATE_CHANGED", runtime, at);
            action.put("from", before.name()); action.put("to", decision.state().name()); actions.add(action);
        }
    }
    private Frame record(Room room, RoomRuntime map, Instant recordedAt, LocalDate date,
                         List<Map<String, Object>> actions) {
        map.markStateChanged(recordedAt);
        long seq = map.nextEventSequence();
        Map<String, Object> snapshot = snapshots.publicSnapshot(room, map, date);
        RoomEvent event = events.save(new RoomEvent(room, seq, recordedAt, map.getSnapshotVersion(), snapshot, actions));
        publisher.publishEvent(event);
        return new Frame(seq, snapshot);
    }
    private Map<String, Object> action(String type, MemberRuntime runtime, Instant at) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", type); action.put("memberId", runtime.getId().toString());
        action.put("effectiveAt", at.toString()); return action;
    }
    private Map<String, Object> endAction(MemberRuntime runtime, Instant at, StudyEndReason reason) {
        Map<String, Object> action = action("STUDY_ENDED", runtime, at);
        action.put("reason", reason.name()); return action;
    }
    private static Instant earlier(Instant a, Instant b) { return a.isBefore(b) ? a : b; }
    private static Instant later(Instant a, Instant b) { return a.isAfter(b) ? a : b; }
    private record Frame(long seq, Map<String, Object> snapshot) {}
}
