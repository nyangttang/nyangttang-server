package com.nyangttang.server.replay.entity;

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
@Table(name = "room_events")
public class RoomEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant occurredAt;

    @Column(name = "snapshot_version", nullable = false)
    private Long snapshotVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> snapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions_json", nullable = false, columnDefinition = "JSON")
    private List<Map<String, Object>> actions;

    protected RoomEvent() {
    }

    public RoomEvent(Room room, long seq, Instant occurredAt, long snapshotVersion,
                     Map<String, Object> snapshot, List<Map<String, Object>> actions) {
        this.room = Objects.requireNonNull(room);
        this.seq = seq;
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.snapshotVersion = snapshotVersion;
        this.snapshot = new HashMap<>(Objects.requireNonNull(snapshot));
        this.actions = new ArrayList<>(Objects.requireNonNull(actions));
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Long getSeq() { return seq; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getSnapshotVersion() { return snapshotVersion; }
    public Map<String, Object> getSnapshot() { return snapshot; }
    public List<Map<String, Object>> getActions() { return actions; }
}
