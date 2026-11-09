package com.nyangttang.server.study.service;

import com.nyangttang.server.device.repository.DeviceRepository;
import com.nyangttang.server.game.repository.*;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.study.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Service
public class StudyGoalService {
    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final RoomRuntimeRepository maps;
    private final MemberRuntimeRepository runtimes;
    private final DeviceRepository devices;
    private final StudyAccountingService accounting;
    private final Clock clock;

    @Autowired
    public StudyGoalService(RoomRepository rooms, RoomMemberRepository members, RoomRuntimeRepository maps,
                            MemberRuntimeRepository runtimes, DeviceRepository devices, StudyAccountingService accounting) {
        this(rooms, members, maps, runtimes, devices, accounting, Clock.systemUTC());
    }
    StudyGoalService(RoomRepository rooms, RoomMemberRepository members, RoomRuntimeRepository maps,
                     MemberRuntimeRepository runtimes, DeviceRepository devices, StudyAccountingService accounting, Clock clock) {
        this.rooms = rooms; this.members = members; this.maps = maps; this.runtimes = runtimes;
        this.devices = devices; this.accounting = accounting; this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UpdateGoalResponse update(Long roomId, Long userId, UpdateGoalRequest request) {
        if (roomId == null || roomId <= 0 || request == null || request.studyDate() == null
                || request.dailyGoalSeconds() == null || request.dailyGoalSeconds() < 30
                || request.dailyGoalSeconds() > 43200) {
            throw error(HttpStatus.BAD_REQUEST, "날짜와 30~43200초 범위의 목표가 필요합니다.");
        }
        var room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."));
        var member = members.findAllByRoom_IdOrderBySlotNoAsc(roomId).stream()
                .filter(m -> m.getUser().getId().equals(userId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "참여 중인 방에서 본인 목표만 변경할 수 있습니다."));
        var runtime = runtimes.findById(member.getId())
                .orElseThrow(() -> new IllegalStateException("고양이 초기 데이터 누락"));
        var map = maps.findById(roomId).orElseThrow(() -> new IllegalStateException("지도 초기 데이터 누락"));
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        if (now.isBefore(runtime.getLastAccountedAt())) now = runtime.getLastAccountedAt();
        // Check after obtaining the lock, so a request waiting across midnight cannot edit another day.
        if (!request.studyDate().equals(StudyTimeline.date(now))) {
            throw error(HttpStatus.CONFLICT, "오늘 날짜가 변경되었습니다. 현재 지도를 새로 조회해주세요.");
        }
        var device = devices.findByMember_Id(member.getId()).orElse(null);
        if (device != null) accounting.advance(room, map, runtime, device, now);
        var stat = accounting.updateGoal(room, map, runtime, request.dailyGoalSeconds(), now);
        long remainingMs = Math.max(0L, stat.getGoalSeconds().longValue() * 1000 - stat.getCreditedMs());
        return new UpdateGoalResponse(roomId.toString(), member.getId().toString(), stat.getStudyDate(),
                stat.getGoalSeconds(), stat.getCreditedMs(), (remainingMs + 999) / 1000,
                stat.isGoalAchieved(), stat.getGoalAchievedAt(), map.getSnapshotVersion(), map.getLastEventSeq());
    }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
