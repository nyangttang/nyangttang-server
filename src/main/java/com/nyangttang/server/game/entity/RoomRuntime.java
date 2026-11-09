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

    public void markStateChanged(Instant now) {
        snapshotVersion++;
        updatedAt = Objects.requireNonNull(now);
    }


    public long nextEventSequence() {
        return ++lastEventSeq;
    }


    public List<Integer> releaseMember(String memberId) {
        List<Integer> released=new ArrayList<>();
        List<String> nextOwners=new ArrayList<>(owners);
        Map<String,Map<String,Integer>> nextVisits=new HashMap<>();
        for(int i=0;i<nextOwners.size();i++) if(memberId.equals(nextOwners.get(i))) {
            nextOwners.set(i,null);released.add(i);
        }
        visits.forEach((cell,attackers)->{
            if(released.contains(Integer.parseInt(cell)))return;
            Map<String,Integer> remaining=new HashMap<>(attackers);remaining.remove(memberId);
            if(!remaining.isEmpty())nextVisits.put(cell,remaining);
        });
        owners=nextOwners;visits=nextVisits;return List.copyOf(released);
    }

    public record TerritoryChange(String type, int cellIndex, String previousOwnerId, String ownerId) {}

    public TerritoryChange visitCell(int x, int y, String visitorId) {
        Objects.requireNonNull(visitorId);
        if (visitorId.isBlank() || x < 0 || x > 9 || y < 0 || y > 9 || owners.size() != 100) {
            throw new IllegalArgumentException("점령 판정 입력이 잘못되었습니다.");
        }
        int index = y * 10 + x;
        String before = owners.get(index);
        if (visitorId.equals(before)) return new TerritoryChange("NONE", index, before, before);

        // Replace JSON collections instead of relying on nested in-place mutations.
        List<String> nextOwners = new ArrayList<>(owners);
        Map<String, Map<String, Integer>> nextVisits = new HashMap<>();
        visits.forEach((cell, attackers) -> nextVisits.put(cell, new HashMap<>(attackers)));
        String key = Integer.toString(index);
        String type;
        if (before == null) {
            nextOwners.set(index, visitorId);
            nextVisits.remove(key);
            type = "CLAIM";
        } else {
            Map<String, Integer> attackers = nextVisits.computeIfAbsent(key, ignored -> new HashMap<>());
            if (attackers.getOrDefault(visitorId, 0) >= 1) {
                nextOwners.set(index, visitorId);
                nextVisits.remove(key); // Clear every attacker's marks after ownership changes.
                type = "INVADE";
            } else {
                attackers.put(visitorId, 1);
                type = "TERRITORY_MARKED";
            }
        }
        owners = nextOwners;
        visits = nextVisits;
        return new TerritoryChange(type, index, before, owners.get(index));
    }

}
