package com.nyangttang.server.device.service;

import com.nyangttang.server.device.dto.MyDeviceResponse;
import com.nyangttang.server.device.repository.DeviceRepository;
import com.nyangttang.server.game.repository.MemberRuntimeRepository;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.study.service.StudyStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class MyDeviceQueryService {
    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final DeviceRepository devices;
    private final MemberRuntimeRepository runtimes;
    private final StudyStateMachine machine;
    private final Clock clock;

    @Autowired
    public MyDeviceQueryService(RoomRepository rooms, RoomMemberRepository members,
            DeviceRepository devices, MemberRuntimeRepository runtimes, StudyStateMachine machine) {
        this(rooms, members, devices, runtimes, machine, Clock.systemUTC());
    }

    // A separate constructor allows exact timeout-boundary tests without a dev-only Clock bean.
    MyDeviceQueryService(RoomRepository rooms, RoomMemberRepository members, DeviceRepository devices,
                         MemberRuntimeRepository runtimes, StudyStateMachine machine, Clock clock) {
        this.rooms = rooms; this.members = members; this.devices = devices;
        this.runtimes = runtimes; this.machine = machine; this.clock = clock;
    }

    public MyDeviceResponse findMyDevice(Long roomId, Long userId) {
        var room = rooms.findById(roomId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."));
        var member = members.findByUser_Id(userId).filter(m -> roomId.equals(m.getRoom().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 방만 조회할 수 있습니다."));
        var device = devices.findByMember_Id(member.getId()).orElse(null);
        if (device == null) return null;
        var runtime = runtimes.findById(member.getId())
                .orElseThrow(() -> new IllegalStateException("고양이 초기 데이터 누락"));
        Instant now = clock.instant();
        Instant seen = device.getLastSeenAt();
        boolean connected = seen != null && now.isBefore(seen.plusMillis(StudyStateMachine.OFFLINE_TIMEOUT_MS));
        // Derive display state without extending heartbeat or changing study/game records.
        var state = machine.tick(runtime.getState(), runtime.getAbsentSince(), seen, now, room.getAwayGraceMs()).state();
        return new MyDeviceResponse(device.getDeviceCode(), member.getId().toString(), device.getSource(),
                seen, connected, state);
    }
}
