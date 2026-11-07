package com.nyangttang.server.device.entity;

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
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_code", nullable = false, length = 32)
    private String deviceCode;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", nullable = true)
    private RoomMember member;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "claim_code_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String claimCodeHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source", nullable = false, length = 12)
    private DeviceSource source;

    @Column(name = "firmware_version", nullable = true, length = 20)
    private String firmwareVersion;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "link_id", nullable = true, length = 8, columnDefinition = "CHAR(8)")
    private String linkId;

    @Column(name = "last_seq", nullable = false)
    private Long lastSeq = -1L;

    @Column(name = "phone_present", nullable = true, columnDefinition = "BOOLEAN")
    private Boolean phonePresent;

    @Column(name = "person_present", nullable = true, columnDefinition = "BOOLEAN")
    private Boolean personPresent;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_seen_at", nullable = true, columnDefinition = "DATETIME(3)")
    private Instant lastSeenAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "claimed_at", nullable = true, columnDefinition = "DATETIME(3)")
    private Instant claimedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private Instant createdAt = Instant.now();

    protected Device() {
    }

    public Device(String deviceCode, String claimCodeHash, DeviceSource source) {
        this.deviceCode = Objects.requireNonNull(deviceCode);
        this.claimCodeHash = Objects.requireNonNull(claimCodeHash);
        this.source = Objects.requireNonNull(source);
    }

    public Long getId() { return id; }
    public String getDeviceCode() { return deviceCode; }
    public RoomMember getMember() { return member; }
    public String getClaimCodeHash() { return claimCodeHash; }
    public DeviceSource getSource() { return source; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public String getLinkId() { return linkId; }
    public Long getLastSeq() { return lastSeq; }
    public Boolean getPhonePresent() { return phonePresent; }
    public Boolean getPersonPresent() { return personPresent; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
