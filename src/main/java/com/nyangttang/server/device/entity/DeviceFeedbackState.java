package com.nyangttang.server.device.entity;

import com.nyangttang.server.device.dto.PhysicalHeartbeatResponse.FaceEvent;
import jakarta.persistence.*;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "device_feedback_states")
public class DeviceFeedbackState {
    @Id
    @Column(name = "device_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "next_room_seq", nullable = false)
    private Long nextRoomSeq;

    @Column(name = "next_action_index", nullable = false)
    private Integer nextActionIndex = 0;

    @Column(name = "last_event_id", nullable = false)
    private Long lastEventId = 0L;

    @Column(name = "last_event_type", length = 12)
    private String lastEventType;

    protected DeviceFeedbackState() {}

    public DeviceFeedbackState(Device device, long currentRoomSeq) {
        this.device = Objects.requireNonNull(device);
        this.nextRoomSeq = Math.addExact(currentRoomSeq, 1L);
    }

    public Long getId() { return id; }
    public Long getNextRoomSeq() { return nextRoomSeq; }
    public Integer getNextActionIndex() { return nextActionIndex; }

    public void consumed(long seq, int actionIndex, int actionCount) {
        if (seq < nextRoomSeq || (seq == nextRoomSeq && actionIndex < nextActionIndex)
                || actionIndex < 0 || actionIndex >= actionCount) {
            throw new IllegalArgumentException("표정 이벤트 순서가 잘못되었습니다.");
        }
        nextRoomSeq = actionIndex + 1 == actionCount ? Math.addExact(seq, 1L) : seq;
        nextActionIndex = actionIndex + 1 == actionCount ? 0 : actionIndex + 1;
    }

    public FaceEvent remember(String type) {
        if (!Set.of("INVADE", "HISS", "GOAL").contains(type)) {
            throw new IllegalArgumentException("지원하지 않는 표정 이벤트입니다.");
        }
        // IDs are persistent and monotonic per device, independent of room-event row IDs.
        lastEventId = Math.addExact(lastEventId, 1L);
        lastEventType = type;
        return lastEvent();
    }

    public FaceEvent lastEvent() {
        return lastEventType == null ? null : new FaceEvent(lastEventId, lastEventType);
    }
}
