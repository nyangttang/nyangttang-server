package com.nyangttang.server.replay.repository;

import com.nyangttang.server.replay.entity.RoomEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomEventRepository extends JpaRepository<RoomEvent, Long> {
    // Existing UNIQUE(room_id, seq) index supports this bounded scan.
    java.util.List<RoomEvent> findByRoom_IdAndSeqGreaterThanEqualOrderBySeqAsc(
            Long roomId, Long seq, org.springframework.data.domain.Pageable page);

    @org.springframework.data.jpa.repository.Query("""
            select e from RoomEvent e
            where e.room.id = :roomId and e.seq > :afterSeq and e.seq <= :endSeq
            order by e.seq asc
            """)
    java.util.List<RoomEvent> findReplayPage(
            @org.springframework.data.repository.query.Param("roomId") Long roomId,
            @org.springframework.data.repository.query.Param("afterSeq") long afterSeq,
            @org.springframework.data.repository.query.Param("endSeq") long endSeq,
            org.springframework.data.domain.Pageable page);

    // Summary needs only actions, not all 100-cell snapshots. Read in bounded chunks.
    @org.springframework.data.jpa.repository.Query("""
            select new com.nyangttang.server.replay.repository.RoomActionSlice(e.seq, e.actions)
            from RoomEvent e
            where e.room.id = :roomId and e.seq > :afterSeq and e.seq <= :endSeq
            order by e.seq asc
            """)
    java.util.List<RoomActionSlice> findActionPage(
            @org.springframework.data.repository.query.Param("roomId") Long roomId,
            @org.springframework.data.repository.query.Param("afterSeq") long afterSeq,
            @org.springframework.data.repository.query.Param("endSeq") long endSeq,
            org.springframework.data.domain.Pageable page);
}
