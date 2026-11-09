package com.nyangttang.server.device.service;

import com.nyangttang.server.device.dto.*;
import com.nyangttang.server.device.entity.*;
import com.nyangttang.server.device.repository.DeviceRepository;
import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.repository.*;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.study.entity.StudyState;
import com.nyangttang.server.study.service.StudyStateMachine;
import com.nyangttang.server.study.service.StudyAccountingService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@Profile({"dev", "prod"})
@Transactional(isolation = Isolation.READ_COMMITTED)
public class MockSensorService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final RoomRuntimeRepository maps;
    private final MemberRuntimeRepository runtimes;
    private final DeviceRepository devices;
    private final StudyAccountingService accounting;
    private final Clock clock;

    public MockSensorService(RoomRepository rooms, RoomMemberRepository members,
                             RoomRuntimeRepository maps, MemberRuntimeRepository runtimes,
                             DeviceRepository devices, StudyAccountingService accounting, Clock clock) {
        this.rooms = rooms; this.members = members; this.maps = maps;
        this.runtimes = runtimes; this.devices = devices;
        this.accounting = accounting; this.clock = clock;
    }

    public MockTelemetryResponse connect(Long roomId, Long userId) {
        Context context = ownedContext(roomId, userId);
        Instant now = now();
        Device device = devices.findByMember_Id(context.member().getId())
                .orElseGet(() -> new Device("SIM-" + randomHex(12),
                        randomHex(32), DeviceSource.SIMULATED));
        requireSimulation(device);
        accounting.advance(context.room(), context.map(), context.runtime(), device, now);
        accounting.disconnect(context.room(), context.map(), context.runtime(), now, false);
        device.connectSimulation(context.member(), randomHex(4), now);
        devices.save(device);
        return response(context, device, true, "CONNECTED");
    }

    public MockTelemetryResponse receive(Long roomId, Long userId, MockTelemetryRequest request) {
        Context context = ownedContext(roomId, userId);
        Device device = devices.findByMember_Id(context.member().getId())
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "먼저 모의 기기를 연결해주세요."));
        requireSimulation(device);
        if (device.getLinkId() == null || !device.getLinkId().equals(request.linkId())) {
            throw error(HttpStatus.CONFLICT, "연결 ID가 만료됐습니다. 다시 연결해주세요.");
        }
        Instant now = now();
        // The old heartbeat can already have expired, even before the scheduler runs.
        advance(context.runtime(), context.map(), device, context.room(), now);
        boolean accepted = device.acceptSimulation(request.linkId(), request.seq(),
                request.phonePresent(), request.personPresent(), now);
        if (!accepted) return response(context, device, false, "DUPLICATE_OR_OLD");

        accounting.afterInput(context.room(), context.map(), context.runtime(),
                request.phonePresent(), request.personPresent(), now);
        return response(context, device, true, "ACCEPTED");
    }

    public void tickRoom(Long roomId) {
        Room room = lockRoom(roomId);
        RoomRuntime map = map(roomId);
        Instant now = now();
        for (Device device : devices.findAllByMember_Room_IdOrderByMember_SlotNoAsc(roomId)) {
            MemberRuntime runtime = runtime(device.getMember().getId());
            advance(runtime, map, device, room, now);
        }
    }

    // One scheduler handles both sources; restart never credits server downtime.
    public void resetRoomAfterRestart(Long roomId) {
        Room room = lockRoom(roomId);
        RoomRuntime map = map(roomId);
        Instant now = now();
        for (Device device : devices.findAllByMember_Room_IdOrderByMember_SlotNoAsc(roomId)) {
            accounting.disconnect(room, map, runtime(device.getMember().getId()), now, true);
            if (device.getSource() == DeviceSource.SIMULATED) device.invalidateSimulationConnection();
            else device.invalidatePhysicalConnection();
        }
    }

    @Transactional(readOnly = true)
    public List<Long> connectedRoomIds() {
        return devices.findConnectedRoomIds();
    }

    private Context ownedContext(Long roomId, Long userId) {
        Room room = lockRoom(roomId);
        RoomMember member = members.findAllByRoom_IdOrderBySlotNoAsc(roomId).stream()
                .filter(m -> m.getUser().getId().equals(userId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "참여 중인 방에서만 사용할 수 있습니다."));
        return new Context(room, member, runtime(member.getId()), map(roomId));
    }
    private Room lockRoom(Long id) {
        return rooms.findByIdForUpdate(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."));
    }
    private RoomRuntime map(Long id) {
        return maps.findById(id).orElseThrow(() -> new IllegalStateException("지도 초기 데이터 누락"));
    }
    private MemberRuntime runtime(Long id) {
        return runtimes.findById(id).orElseThrow(() -> new IllegalStateException("고양이 초기 데이터 누락"));
    }
    private void requireSimulation(Device device) {
        if (device.getSource() != DeviceSource.SIMULATED) {
            throw error(HttpStatus.CONFLICT, "실제 기기가 연결된 참가자입니다.");
        }
    }
    private void advance(MemberRuntime runtime, RoomRuntime map, Device device, Room room, Instant now) {
        accounting.advance(room, map, runtime, device, now);
    }
    private MockTelemetryResponse response(Context c, Device d, boolean accepted, String reason) {
        return new MockTelemetryResponse(accepted, reason, d.getDeviceCode(), d.getLinkId(),
                d.getLastSeq(), c.member().getId().toString(), c.runtime().getState(),
                c.runtime().getAbsentSince(), d.getLastSeenAt(), c.room().getAwayGraceMs(),
                StudyStateMachine.OFFLINE_TIMEOUT_MS);
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MILLIS); }
    private static String randomHex(int count) {
        byte[] bytes = new byte[count]; RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
    private record Context(Room room, RoomMember member, MemberRuntime runtime, RoomRuntime map) {}
}
