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
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private RoomMember member;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant startedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "ended_at", nullable = true, columnDefinition = "DATETIME(3)")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "end_reason", nullable = true, length = 20)
    private StudyEndReason endReason;

    @Column(name = "credited_ms", nullable = false)
    private Long creditedMs = 0L;

    @Column(name = "start_event_seq", nullable = false)
    private Long startEventSeq;

    @Column(name = "end_event_seq", nullable = true)
    private Long endEventSeq;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "start_snapshot_json", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> startSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "end_snapshot_json", nullable = true, columnDefinition = "JSON")
    private Map<String, Object> endSnapshot;

    protected StudySession() {
    }

    public StudySession(RoomMember member, LocalDate studyDate, Instant startedAt,
                        long startEventSeq, Map<String, Object> startSnapshot) {
        this.member = Objects.requireNonNull(member);
        this.studyDate = Objects.requireNonNull(studyDate);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.startEventSeq = startEventSeq;
        this.startSnapshot = new HashMap<>(Objects.requireNonNull(startSnapshot));
    }

    public Long getId() { return id; }
    public RoomMember getMember() { return member; }
    public LocalDate getStudyDate() { return studyDate; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public StudyEndReason getEndReason() { return endReason; }
    public Long getCreditedMs() { return creditedMs; }
    public Long getStartEventSeq() { return startEventSeq; }
    public Long getEndEventSeq() { return endEventSeq; }
    public Map<String, Object> getStartSnapshot() { return startSnapshot; }
    public Map<String, Object> getEndSnapshot() { return endSnapshot; }
}
