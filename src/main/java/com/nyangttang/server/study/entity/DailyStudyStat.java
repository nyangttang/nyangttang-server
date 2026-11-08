package com.nyangttang.server.study.entity;

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
@Table(name = "daily_study_stats")
public class DailyStudyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private RoomMember member;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "credited_ms", nullable = false)
    private Long creditedMs = 0L;

    @Column(name = "goal_seconds", nullable = false)
    private Integer goalSeconds;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "goal_achieved_at", nullable = true, columnDefinition = "DATETIME(3)")
    private Instant goalAchievedAt;

    protected DailyStudyStat() {
    }

    public DailyStudyStat(RoomMember member, LocalDate studyDate, int goalSeconds) {
        this.member = Objects.requireNonNull(member);
        this.studyDate = Objects.requireNonNull(studyDate);
        this.goalSeconds = goalSeconds;
    }

    public Long getId() { return id; }
    public RoomMember getMember() { return member; }
    public LocalDate getStudyDate() { return studyDate; }
    public Long getCreditedMs() { return creditedMs; }
    public Integer getGoalSeconds() { return goalSeconds; }
    public Instant getGoalAchievedAt() { return goalAchievedAt; }

    // The timestamp is historical: the first achievement of the day, even after raising the goal.
    public boolean isGoalAchieved() { return creditedMs >= goalSeconds.longValue() * 1000; }

    public Instant changeGoalSeconds(int seconds, Instant at) {
        if (seconds < 30 || seconds > 43200) throw new IllegalArgumentException("목표는 30~43200초입니다.");
        Objects.requireNonNull(at);
        goalSeconds = seconds;
        if (goalAchievedAt == null && isGoalAchieved()) {
            goalAchievedAt = at;
            return at;
        }
        return null;
    }

    // Returns the exact first goal crossing time, or null if not newly reached.
    public Instant addCredit(long millis, Instant intervalStart) {
        if (millis < 0) throw new IllegalArgumentException("인정 시간은 음수일 수 없습니다.");
        long before = creditedMs;
        creditedMs = Math.addExact(creditedMs, millis);
        long target = goalSeconds.longValue() * 1000;
        if (goalAchievedAt == null && before < target && creditedMs >= target) {
            goalAchievedAt = intervalStart.plusMillis(target - before);
            return goalAchievedAt;
        }
        return null;
    }

}
