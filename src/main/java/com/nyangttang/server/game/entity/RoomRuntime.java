package com.nyangttang.server.game.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import com.nyangttang.server.user.entity.User;
import com.nyangttang.server.room.entity.Room;
import com.nyangttang.server.room.entity.RoomMember;
import com.nyangttang.server.study.entity.StudyState;
import com.nyangttang.server.study.entity.StudyEndReason;

@Entity
@Table(name = "room_runtime")
public class RoomRuntime {

    @Id
    @Column(name = "room_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "snapshot_version", nullable = false)
    private Long snapshotVersion = 0L;

    @Column(name = "last_event_seq", nullable = false)
    private Long lastEventSeq = 0L;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "owners_json", nullable = false, columnDefinition = "JSON")
    private List<String> owners = new ArrayList<>(Collections.nCopies(100, null));

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visits_json", nullable = false, columnDefinition = "JSON")
    private Map<String, Map<String, Integer>> visits = new HashMap<>();

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant updatedAt = Instant.now();

    protected RoomRuntime() {
    }

    public RoomRuntime(Room room) {
        this.room = Objects.requireNonNull(room);
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Long getRowVersion() { return rowVersion; }
    public Long getSnapshotVersion() { return snapshotVersion; }
    public Long getLastEventSeq() { return lastEventSeq; }
    public List<String> getOwners() { return owners; }
    public Map<String, Map<String, Integer>> getVisits() { return visits; }
    public Instant getUpdatedAt() { return updatedAt; }
}
