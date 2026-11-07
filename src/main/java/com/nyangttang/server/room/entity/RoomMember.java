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
@Table(name = "room_members")
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "slot_no", nullable = false)
    private Byte slotNo;

    @Column(name = "cat_name", nullable = false, length = 12)
    private String catName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cat_color", nullable = false, length = 10)
    private CatColor catColor;

    @Column(name = "daily_goal_seconds", nullable = false)
    private Integer dailyGoalSeconds;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "joined_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant joinedAt = Instant.now();

    protected RoomMember() {
    }

    public RoomMember(Room room, User user, byte slotNo, String catName,
                      CatColor catColor, int dailyGoalSeconds) {
        this.room = Objects.requireNonNull(room);
        this.user = Objects.requireNonNull(user);
        this.slotNo = slotNo;
        this.catName = Objects.requireNonNull(catName);
        this.catColor = Objects.requireNonNull(catColor);
        this.dailyGoalSeconds = dailyGoalSeconds;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public User getUser() { return user; }
    public Byte getSlotNo() { return slotNo; }
    public String getCatName() { return catName; }
    public CatColor getCatColor() { return catColor; }
    public Integer getDailyGoalSeconds() { return dailyGoalSeconds; }
    public Instant getJoinedAt() { return joinedAt; }
}
