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
}
