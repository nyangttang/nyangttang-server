package com.nyangttang.server.notification.entity;

import com.nyangttang.server.room.entity.Room;
import com.nyangttang.server.room.entity.RoomMember;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "room_notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_event_action", columnNames = {"room_id", "event_seq", "action_index"}))
public class RoomNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_member_id", nullable = false)
    private RoomMember recipient;
    @Column(name = "event_seq", nullable = false) private Long eventSeq;
    @Column(name = "action_index", nullable = false) private Integer actionIndex;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 24) private NotificationType type;
    @Column(name = "actor_member_id") private Long actorMemberId;
    @Column(name = "cell_index") private Integer cellIndex;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "effective_at", nullable = false, columnDefinition = "DATETIME(3)") private Instant effectiveAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "read_at", columnDefinition = "DATETIME(3)") private Instant readAt;

    protected RoomNotification() {}
    public RoomNotification(Room room, RoomMember recipient, long eventSeq, int actionIndex,
                            NotificationType type, Long actorMemberId, Integer cellIndex,
                            Instant effectiveAt, Instant createdAt) {
        this.room = Objects.requireNonNull(room); this.recipient = Objects.requireNonNull(recipient);
        this.type = Objects.requireNonNull(type); this.effectiveAt = Objects.requireNonNull(effectiveAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        if (!room.getId().equals(recipient.getRoom().getId()) || eventSeq < 1 || actionIndex < 0)
            throw new IllegalArgumentException("알림 이벤트와 수신자 정보가 유효하지 않습니다.");
        if (type == NotificationType.LAND_LOST && (actorMemberId == null || actorMemberId < 1
                || actorMemberId.equals(recipient.getId()) || cellIndex == null || cellIndex < 0 || cellIndex > 99))
            throw new IllegalArgumentException("땅 상실 알림 정보가 유효하지 않습니다.");
        if (type == NotificationType.AWAY_TOO_LONG && (actorMemberId != null || cellIndex != null))
            throw new IllegalArgumentException("자리 비움 알림에 점령 정보가 들어갈 수 없습니다.");
        this.eventSeq = eventSeq; this.actionIndex = actionIndex;
        this.actorMemberId = actorMemberId; this.cellIndex = cellIndex;
    }
    public void markRead(Instant now) { if (readAt == null) readAt = Objects.requireNonNull(now); }
    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public RoomMember getRecipient() { return recipient; }
    public Long getEventSeq() { return eventSeq; }
    public Integer getActionIndex() { return actionIndex; }
    public NotificationType getType() { return type; }
    public Long getActorMemberId() { return actorMemberId; }
    public Integer getCellIndex() { return cellIndex; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
