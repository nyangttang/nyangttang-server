package com.nyangttang.server.room.service;

import com.nyangttang.server.game.entity.RoomRuntime;
import com.nyangttang.server.game.repository.RoomRuntimeRepository;
import com.nyangttang.server.room.dto.RoomSnapshotResponse;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.study.service.StudyTimeline;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class RoomSnapshotService {
    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final RoomRuntimeRepository maps;
    private final RoomSnapshotAssembler assembler;
    public RoomSnapshotService(RoomRepository rooms, RoomMemberRepository members,
                               RoomRuntimeRepository maps, RoomSnapshotAssembler assembler) {
        this.rooms = rooms; this.members = members; this.maps = maps; this.assembler = assembler;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RoomSnapshotResponse getSnapshot(Long roomId, Long userId) {
        Instant now = Instant.now();
        Room room = rooms.findById(roomId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."));
        RoomMember me = members.findAllByRoom_IdOrderBySlotNoAsc(roomId).stream()
                .filter(m -> m.getUser().getId().equals(userId)).findFirst().orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 방만 조회할 수 있습니다."));
        RoomRuntime map = maps.findById(roomId).orElseThrow(() -> new IllegalStateException("지도 초기 데이터 누락"));
        return assembler.build(room, map, me.getId().toString(), StudyTimeline.date(now));
    }
}
