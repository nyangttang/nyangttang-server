package com.nyangttang.server.room.service;

import com.nyangttang.server.room.dto.MyRoomResponse;
import com.nyangttang.server.room.repository.RoomMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyRoomQueryService {
    private final RoomMemberRepository members;
    public MyRoomQueryService(RoomMemberRepository members) { this.members = members; }

    public MyRoomResponse findMyRoom(Long userId) {
        return members.findByUser_Id(userId).map(MyRoomResponse::from).orElse(null);
    }
}
