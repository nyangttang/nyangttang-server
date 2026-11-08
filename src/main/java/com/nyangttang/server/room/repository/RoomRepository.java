package com.nyangttang.server.room.repository;

import com.nyangttang.server.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByInviteCode(String inviteCode);
}