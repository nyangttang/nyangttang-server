package com.nyangttang.server.room.service;

import com.nyangttang.server.game.entity.MemberRuntime;
import com.nyangttang.server.game.entity.RoomRuntime;
import com.nyangttang.server.game.repository.MemberRuntimeRepository;
import com.nyangttang.server.game.repository.RoomRuntimeRepository;
import com.nyangttang.server.room.dto.RoomSnapshotResponse;
import com.nyangttang.server.room.entity.Room;
import com.nyangttang.server.room.entity.RoomMember;
import com.nyangttang.server.room.repository.RoomMemberRepository;
import com.nyangttang.server.room.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomSnapshotService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRuntimeRepository roomRuntimeRepository;
    private final MemberRuntimeRepository memberRuntimeRepository;

    public RoomSnapshotService(
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomRuntimeRepository roomRuntimeRepository,
            MemberRuntimeRepository memberRuntimeRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomRuntimeRepository = roomRuntimeRepository;
        this.memberRuntimeRepository = memberRuntimeRepository;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public RoomSnapshotResponse getSnapshot(
            Long roomId,
            Long userId
    ) {
        // 1. 방이 존재하는지 확인합니다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "방을 찾을 수 없습니다."
                ));

        List<RoomMember> members =
                roomMemberRepository.findAllByRoom_IdOrderBySlotNoAsc(
                        roomId
                );

        // 2. 요청한 사용자가 이 방의 참가자인지 확인합니다.
        RoomMember me = members.stream()
                .filter(member ->
                        member.getUser().getId().equals(userId)
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "참여 중인 방만 조회할 수 있습니다."
                ));

        // 3. 현재 지도를 읽습니다.
        RoomRuntime roomRuntime = roomRuntimeRepository.findById(roomId)
                .orElseThrow(() -> new IllegalStateException(
                        "방의 지도 초기 데이터가 없습니다."
                ));

        // 빈 칸은 null이므로 null을 허용하는 ArrayList로 복사합니다.
        List<String> owners = new ArrayList<>(
                roomRuntime.getOwners()
        );

        if (owners.size() != 100) {
            throw new IllegalStateException(
                    "지도는 100칸이어야 합니다."
            );
        }

        // 4. 고양이 상태를 참가자 번호로 찾을 수 있게 정리합니다.
        List<MemberRuntime> runtimes =
                memberRuntimeRepository.findAllByRoomId(roomId);

        Map<Long, MemberRuntime> runtimeByMemberId = new HashMap<>();

        for (MemberRuntime runtime : runtimes) {
            runtimeByMemberId.put(runtime.getId(), runtime);
        }

        // 5. 소유자별 현재 땅 개수를 계산합니다.
        Map<String, Integer> ownedCounts = new HashMap<>();

        for (String ownerId : owners) {
            if (ownerId != null) {
                ownedCounts.merge(ownerId, 1, Integer::sum);
            }
        }

        // 6. 참가자별 응답을 만듭니다.
        List<RoomSnapshotResponse.MemberSnapshot> memberSnapshots =
                new ArrayList<>();

        for (RoomMember member : members) {
            MemberRuntime runtime =
                    runtimeByMemberId.get(member.getId());

            if (runtime == null) {
                throw new IllegalStateException(
                        "참가자의 고양이 초기 상태가 없습니다."
                );
            }

            String memberId = member.getId().toString();

            memberSnapshots.add(
                    new RoomSnapshotResponse.MemberSnapshot(
                            memberId,
                            member.getSlotNo(),
                            member.getCatName(),
                            member.getCatColor(),
                            runtime.getState(),
                            runtime.getX(),
                            runtime.getY(),
                            ownedCounts.getOrDefault(memberId, 0),
                            member.getDailyGoalSeconds()
                    )
            );
        }

        return new RoomSnapshotResponse(
                1,
                room.getId().toString(),
                room.getName(),
                room.getMode(),
                10,
                10,
                roomRuntime.getSnapshotVersion(),
                roomRuntime.getLastEventSeq(),
                me.getId().toString(),
                owners,
                memberSnapshots
        );
    }
}