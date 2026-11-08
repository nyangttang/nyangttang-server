package com.nyangttang.server.room.repository;

import com.nyangttang.server.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMemberRepository
        extends JpaRepository<RoomMember, Long> {

    boolean existsByUser_Id(Long userId);

    List<RoomMember> findAllByRoom_IdOrderBySlotNoAsc(Long roomId);
}