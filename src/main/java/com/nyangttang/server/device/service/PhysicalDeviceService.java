package com.nyangttang.server.device.service;

import com.nyangttang.server.device.dto.*;
import com.nyangttang.server.device.entity.*;
import com.nyangttang.server.device.repository.*;
import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.repository.*;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.study.repository.DailyStudyStatRepository;
import com.nyangttang.server.study.service.StudyAccountingService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@Profile("dev")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PhysicalDeviceService {
    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final RoomRuntimeRepository maps;
    private final MemberRuntimeRepository runtimes;
    private final DeviceRepository devices;
    private final DeviceFeedbackStateRepository feedbackStates;
    private final DailyStudyStatRepository daily;
    private final StudyAccountingService accounting;
    private final DeviceFeedbackService feedback;
    private final Clock clock;

    public PhysicalDeviceService(RoomRepository rooms, RoomMemberRepository members,
                                 RoomRuntimeRepository maps, MemberRuntimeRepository runtimes, DeviceRepository devices,
                                 DeviceFeedbackStateRepository feedbackStates, DailyStudyStatRepository daily,
                                 StudyAccountingService accounting, DeviceFeedbackService feedback, Clock clock) {
        this.rooms = rooms; this.members = members; this.maps = maps; this.runtimes = runtimes;
        this.devices = devices; this.feedbackStates = feedbackStates; this.daily = daily;
        this.accounting = accounting; this.feedback = feedback; this.clock = clock;
    }

    public RegisterPhysicalDeviceResponse register(Long roomId, Long userId,
                                                   RegisterPhysicalDeviceRequest request) {
        Room room = lockRoom(roomId);
        RoomMember member = members.findAllByRoom_IdOrderBySlotNoAsc(roomId).stream()
                .filter(m -> m.getUser().getId().equals(userId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "참여 중인 방에서만 등록할 수 있습니다."));
        String code = request.deviceCode();
        if (code.startsWith("SIM-")) throw error(HttpStatus.BAD_REQUEST, "SIM-은 모의 기기 전용 코드입니다.");
        Device current = devices.findByMember_Id(member.getId()).orElse(null);
        Device existing = devices.findByDeviceCode(code).orElse(null);
        if (existing != null && (current == null || !existing.getId().equals(current.getId()))) {
            throw error(HttpStatus.CONFLICT, "이미 사용 중인 기기 코드입니다.");
        }
        RoomRuntime map = map(roomId);
        if (current != null && current.getSource() == DeviceSource.PHYSICAL) {
            if (!current.getDeviceCode().equals(code)) {
                throw error(HttpStatus.CONFLICT, "이미 다른 실제 기기가 등록되어 있습니다.");
            }
            // Idempotent retry must not reset study state or the event cursor.
            ensureFeedback(current, map);
            return registration(current);
        }
        MemberRuntime runtime = runtime(member.getId());
        Instant now = now(runtime);
        if (current == null) {
            byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
            // Schema placeholder only; NOT used as device authentication in this legacy protocol.
            current = new Device(code, HexFormat.of().formatHex(random), DeviceSource.PHYSICAL);
        }
        accounting.advance(room, map, runtime, current, now);
        accounting.disconnect(room, map, runtime, now, false);
        // Reuse a simulated device row: member_id is UNIQUE. All game/study records remain intact.
        current.bindPhysical(member, code, now);
        devices.saveAndFlush(current);
        ensureFeedback(current, map);
        return registration(current);
    }

    public PhysicalHeartbeatResponse heartbeat(String code, PhysicalHeartbeatRequest request) {
        Long roomId = devices.findRoomIdByCode(code, DeviceSource.PHYSICAL)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "등록된 실제 기기를 찾을 수 없습니다."));
        Room room = lockRoom(roomId);
        Device device = devices.findByDeviceCode(code)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "기기를 찾을 수 없습니다."));
        if (device.getSource() != DeviceSource.PHYSICAL || device.getMember() == null
                || !roomId.equals(device.getMember().getRoom().getId())) {
            throw error(HttpStatus.CONFLICT, "기기 연결이 변경되었습니다.");
        }
        RoomMember member = device.getMember();
        MemberRuntime runtime = runtime(member.getId());
        RoomRuntime map = map(roomId);
        DeviceFeedbackState cursor = feedbackStates.findById(device.getId())
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "기기 등록 API를 먼저 호출해주세요."));
        Instant now = now(runtime);
        // Settle the old sensor interval BEFORE refreshing lastSeenAt (including any timeout).
        accounting.advance(room, map, runtime, device, now);
        device.acceptPhysical(request.phone(), request.seat(), now);
        accounting.afterInput(room, map, runtime, request.phone(), request.seat(), now);
        // distanceCm is diagnostic only. seat is the firmware's occupancy decision.
        var stat = daily.findByMember_IdAndStudyDate(member.getId(),
                now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()).orElse(null);
        var event = feedback.next(roomId, member.getId().toString(), cursor,
                now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate(), stat != null && stat.isGoalAchieved());
        return PhysicalResponseFactory.create(runtime.getState(), map.getOwners(),
                member.getId().toString(), stat == null ? member.getDailyGoalSeconds() : stat.getGoalSeconds(),
                stat == null ? 0L : stat.getCreditedMs(), event);
    }

    private void ensureFeedback(Device device, RoomRuntime map) {
        if (feedbackStates.findById(device.getId()).isEmpty()) {
            feedbackStates.save(new DeviceFeedbackState(device, map.getLastEventSeq()));
        }
    }
    private RegisterPhysicalDeviceResponse registration(Device device) {
        return new RegisterPhysicalDeviceResponse(device.getDeviceCode(), device.getMember().getId().toString(),
                "PHYSICAL", "/api/devices/" + device.getDeviceCode() + "/heartbeat");
    }
    private Instant now(MemberRuntime runtime) {
        Instant time = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        return time.isBefore(runtime.getLastAccountedAt()) ? runtime.getLastAccountedAt() : time;
    }
    private Room lockRoom(Long id) {
        return rooms.findByIdForUpdate(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."));
    }
    private RoomRuntime map(Long id) {
        return maps.findById(id).orElseThrow(() -> new IllegalStateException("지도 초기 데이터 누락"));
    }
    private MemberRuntime runtime(Long id) {
        return runtimes.findById(id).orElseThrow(() -> new IllegalStateException("고양이 초기 데이터 누락"));
    }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
