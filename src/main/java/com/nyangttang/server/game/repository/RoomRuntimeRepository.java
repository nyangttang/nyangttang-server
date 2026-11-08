package com.nyangttang.server.game.repository;

import com.nyangttang.server.game.entity.RoomRuntime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRuntimeRepository
        extends JpaRepository<RoomRuntime, Long> {
}