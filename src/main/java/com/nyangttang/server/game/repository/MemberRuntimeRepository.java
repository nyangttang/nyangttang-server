package com.nyangttang.server.game.repository;

import com.nyangttang.server.game.entity.MemberRuntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberRuntimeRepository
        extends JpaRepository<MemberRuntime, Long> {

    @Query("""
            select runtime
            from MemberRuntime runtime
            where runtime.member.room.id = :roomId
            order by runtime.member.slotNo
            """)
    List<MemberRuntime> findAllByRoomId(
            @Param("roomId") Long roomId
    );
}