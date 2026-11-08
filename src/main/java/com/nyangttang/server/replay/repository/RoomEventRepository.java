package com.nyangttang.server.replay.repository;

import com.nyangttang.server.replay.entity.RoomEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomEventRepository extends JpaRepository<RoomEvent, Long> {
    // Existing UNIQUE(room_id, seq) index supports this bounded scan.
    java.util.List<RoomEvent> findByRoom_IdAndSeqGreaterThanEqualOrderBySeqAsc(
            Long roomId, Long seq, org.springframework.data.domain.Pageable page);
}
