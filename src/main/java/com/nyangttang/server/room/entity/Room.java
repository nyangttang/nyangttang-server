package com.nyangttang.server.room.entity;

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
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "invite_code", nullable = false, length = 6, columnDefinition = "CHAR(6)")
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "mode", nullable = false, length = 10)
    private RoomMode mode;

    @Column(name = "step_ms", nullable = false)
    private Integer stepMs;

    @Column(name = "away_grace_ms", nullable = false)
    private Integer awayGraceMs;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant createdAt = Instant.now();

    protected Room() {
    }

    public Room(String name, String inviteCode, User createdBy, RoomMode mode) {
        this.name = Objects.requireNonNull(name);
        this.inviteCode = Objects.requireNonNull(inviteCode);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.mode = Objects.requireNonNull(mode);
        this.stepMs = mode == RoomMode.NORMAL ? 60_000 : 5_000;
        this.awayGraceMs = mode == RoomMode.NORMAL ? 120_000 : 5_000;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public User getCreatedBy() { return createdBy; }
    public RoomMode getMode() { return mode; }
    public Integer getStepMs() { return stepMs; }
    public Integer getAwayGraceMs() { return awayGraceMs; }
    public Instant getCreatedAt() { return createdAt; }
}
