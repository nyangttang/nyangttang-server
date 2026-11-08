package com.nyangttang.server.room.repository;

import com.nyangttang.server.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository
        extends JpaRepository<RoomMember, Long> {

    boolean existsByUser_Id(Long userId);
}