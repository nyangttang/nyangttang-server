package com.nyangttang.server.room.service;

import com.nyangttang.server.game.entity.MemberRuntime;
import com.nyangttang.server.game.entity.RoomRuntime;
import com.nyangttang.server.game.repository.MemberRuntimeRepository;
import com.nyangttang.server.game.repository.RoomRuntimeRepository;
import com.nyangttang.server.room.dto.CreateRoomRequest;
import com.nyangttang.server.room.dto.CreateRoomResponse;
import com.nyangttang.server.room.entity.Room;
import com.nyangttang.server.room.entity.RoomMember;
import com.nyangttang.server.room.repository.RoomMemberRepository;
import com.nyangttang.server.room.repository.RoomRepository;
import com.nyangttang.server.user.entity.User;
import com.nyangttang.server.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nyangttang.server.room.dto.JoinRoomRequest;
import com.nyangttang.server.room.dto.JoinRoomResponse;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class RoomService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRuntimeRepository roomRuntimeRepository;
    private final MemberRuntimeRepository memberRuntimeRepository;
    private final UserRepository userRepository;

    public RoomService(
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomRuntimeRepository roomRuntimeRepository,
            MemberRuntimeRepository memberRuntimeRepository,
            UserRepository userRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomRuntimeRepository = roomRuntimeRepository;
        this.memberRuntimeRepository = memberRuntimeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateRoomResponse createRoom(
            Long userId,
            CreateRoomRequest request
    ) {
        // 같은 사용자의 방 생성 요청을 순서대로 처리합니다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "사용자를 찾을 수 없습니다."
                ));

        if (roomMemberRepository.existsByUser_Id(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 참여 중인 방이 있습니다."
            );
        }

        Room room = new Room(
                request.name(),
                generateInviteCode(),
                user,
                request.mode()
        );

        // 다른 요청이 같은 초대 코드를 먼저 저장하는 경우도 DB가 차단합니다.
        try {
            roomRepository.saveAndFlush(room);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "방 생성 정보가 충돌했습니다. 다시 요청해주세요.",
                    e
            );
        }

        RoomMember host = new RoomMember(
                room,
                user,
                (byte) 0,
                request.catName(),
                request.catColor(),
                request.dailyGoalSeconds()
        );

        roomMemberRepository.save(host);

        // 기존 생성자가 소유자 없는 100칸과 빈 방문 기록을 만듭니다.
        RoomRuntime roomRuntime = new RoomRuntime(room);
        roomRuntimeRepository.save(roomRuntime);

        // 기기 입력 전이므로 초기 상태는 OFFLINE입니다.
        MemberRuntime memberRuntime = new MemberRuntime(
                host,
                (byte) 0,
                (byte) 0,
                Instant.now()
        );

        memberRuntimeRepository.save(memberRuntime);

        return new CreateRoomResponse(
                room.getId(),
                room.getName(),
                room.getInviteCode(),
                room.getMode(),
                room.getStepMs(),
                room.getAwayGraceMs(),
                host.getId(),
                host.getCatName(),
                host.getCatColor(),
                host.getDailyGoalSeconds(),
                memberRuntime.getState(),
                memberRuntime.getX(),
                memberRuntime.getY()
        );
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(6);

            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARACTERS.charAt(
                        RANDOM.nextInt(CODE_CHARACTERS.length())
                ));
            }

            String inviteCode = code.toString();

            if (!roomRepository.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "초대 코드를 생성하지 못했습니다. 다시 요청해주세요."
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JoinRoomResponse joinRoom(
            Long userId,
            JoinRoomRequest request
    ) {
        // 1. 같은 사용자의 중복 요청을 순서대로 처리합니다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "사용자를 찾을 수 없습니다."
                ));

        if (roomMemberRepository.existsByUser_Id(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 참여 중인 방이 있습니다."
            );
        }

        // 2. 같은 방의 입장 요청도 순서대로 처리합니다.
        Room room = roomRepository
                .findByInviteCodeForUpdate(request.inviteCode())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "초대 코드에 해당하는 방이 없습니다."
                ));

        // 방 잠금을 얻은 뒤 참가자 목록을 읽습니다.
        List<RoomMember> members =
                roomMemberRepository.findAllByRoom_IdOrderBySlotNoAsc(
                        room.getId()
                );

        // 3. 정원을 확인합니다.
        if (members.size() >= 4) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "방 정원이 가득 찼습니다."
            );
        }

        // 4. 고양이 색 중복을 확인합니다.
        boolean colorTaken = members.stream()
                .anyMatch(member ->
                        member.getCatColor() == request.catColor()
                );

        if (colorTaken) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 고양이 색입니다."
            );
        }

        // 5. 비어 있는 가장 작은 자리 번호를 찾습니다.
        boolean[] occupied = new boolean[4];

        for (RoomMember member : members) {
            occupied[member.getSlotNo()] = true;
        }

        int slotNo = 0;

        while (slotNo < occupied.length && occupied[slotNo]) {
            slotNo++;
        }

        if (slotNo == occupied.length) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "입장 가능한 자리가 없습니다."
            );
        }

        // 6. 참가자를 저장합니다.
        RoomMember member = new RoomMember(
                room,
                user,
                (byte) slotNo,
                request.catName(),
                request.catColor(),
                request.dailyGoalSeconds()
        );

        roomMemberRepository.save(member);

        // 7. 자리 번호에 따라 지도 네 모서리에 배치합니다.
        byte x = (byte) (slotNo % 2 == 0 ? 0 : 9);
        byte y = (byte) (slotNo < 2 ? 0 : 9);

        MemberRuntime runtime = new MemberRuntime(
                member,
                x,
                y,
                Instant.now()
        );

        memberRuntimeRepository.save(runtime);

        return new JoinRoomResponse(
                room.getId(),
                room.getName(),
                room.getMode(),
                member.getId(),
                slotNo,
                member.getCatName(),
                member.getCatColor(),
                member.getDailyGoalSeconds(),
                runtime.getState(),
                x,
                y
        );
    }
}