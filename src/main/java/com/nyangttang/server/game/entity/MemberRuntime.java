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
@Table(name = "member_runtime")
public class MemberRuntime {

    @Id
    @Column(name = "member_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private RoomMember member;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "state", nullable = false, length = 20)
    private StudyState state;

    @Column(name = "x", nullable = false)
    private Byte x;

    @Column(name = "y", nullable = false)
    private Byte y;

    @Column(name = "previous_x", nullable = true)
    private Byte previousX;

    @Column(name = "previous_y", nullable = true)
    private Byte previousY;

    @Column(name = "step_remainder_ms", nullable = false)
    private Integer stepRemainderMs = 0;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "absent_since", nullable = true, columnDefinition = "DATETIME(3)")
    private Instant absentSince;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_accounted_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant lastAccountedAt;

    protected MemberRuntime() {
    }

    public MemberRuntime(RoomMember member, byte x, byte y, Instant now) {
        this.member = Objects.requireNonNull(member);
        this.state = StudyState.OFFLINE;
        this.x = x;
        this.y = y;
        this.lastAccountedAt = Objects.requireNonNull(now);
    }

    public Long getId() { return id; }
    public RoomMember getMember() { return member; }
    public StudyState getState() { return state; }
    public Byte getX() { return x; }
    public Byte getY() { return y; }
    public Byte getPreviousX() { return previousX; }
    public Byte getPreviousY() { return previousY; }
    public Integer getStepRemainderMs() { return stepRemainderMs; }
    public Instant getAbsentSince() { return absentSince; }
    public Instant getLastAccountedAt() { return lastAccountedAt; }

    public boolean changeStudyState(StudyState nextState, Instant nextAbsentSince) {
        Objects.requireNonNull(nextState);
        boolean changed = state != nextState || !Objects.equals(absentSince, nextAbsentSince);
        state = nextState;
        absentSince = nextAbsentSince;
        return changed;
    }


    public void accountThrough(Instant at) {
        Objects.requireNonNull(at);
        if (at.isBefore(lastAccountedAt)) {
            throw new IllegalArgumentException("시간 반영 시각은 되돌릴 수 없습니다.");
        }
        lastAccountedAt = at;
    }


    public int earnMovementSteps(long creditedMillis, int stepMillis) {
        if (creditedMillis < 0 || stepMillis <= 0 || stepMillis > 60_000
                || stepRemainderMs < 0 || stepRemainderMs >= stepMillis) {
            throw new IllegalArgumentException("이동 시간 설정이 잘못되었습니다.");
        }
        long total = Math.addExact(stepRemainderMs.longValue(), creditedMillis);
        int steps = Math.toIntExact(total / stepMillis);
        stepRemainderMs = (int) (total % stepMillis);
        return steps;
    }

    public void allowBacktrackAfterEncounter() {
        previousX = null;
        previousY = null;
    }

    public void moveTo(int nextX, int nextY) {
        if (nextX < 0 || nextX > 9 || nextY < 0 || nextY > 9
                || Math.abs(nextX - x) + Math.abs(nextY - y) != 1) {
            throw new IllegalArgumentException("상하좌우 인접 칸으로만 이동할 수 있습니다.");
        }
        previousX = x;
        previousY = y;
        x = (byte) nextX;
        y = (byte) nextY;
    }

}
