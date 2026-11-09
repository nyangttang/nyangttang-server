package com.nyangttang.server.push;

import com.nyangttang.server.notification.entity.RoomNotification;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

@Entity @Table(name="push_deliveries",uniqueConstraints=@UniqueConstraint(name="uk_push_delivery",columnNames={"subscription_id","notification_id"}))
public class PushDelivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subscription_id",nullable=false) private PushSubscription subscription;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="notification_id",nullable=false) private RoomNotification notification;
    @Column(name="subscription_revision",nullable=false) private long subscriptionRevision;
    @Column(name="status",nullable=false,length=16) private String status="PENDING";
    @Column(name="attempts",nullable=false) private int attempts=0;
    @Column(name="lease_token",length=36) private String leaseToken;
    @Column(name="last_status") private Integer lastStatus;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="available_at",nullable=false,columnDefinition="DATETIME(3)") private Instant availableAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="created_at",nullable=false,columnDefinition="DATETIME(3)") private Instant createdAt;
    protected PushDelivery(){}
    public PushDelivery(PushSubscription s,RoomNotification n){subscription=s;notification=n;subscriptionRevision=s.getRevision();createdAt=n.getCreatedAt();availableAt=createdAt;}
    public void claim(Instant now,String token){status="IN_FLIGHT";attempts++;leaseToken=token;availableAt=now.plusSeconds(60);}
    public void finish(String state,Integer code){status=state;lastStatus=code;leaseToken=null;}
    public void retry(Instant at,int code){status="PENDING";availableAt=at;lastStatus=code;leaseToken=null;}
    public boolean ownsLease(String token){return "IN_FLIGHT".equals(status)&&Objects.equals(leaseToken,token);}
    public boolean expired(Instant now){return !now.isBefore(createdAt.plusSeconds(900));}
    public Long getId(){return id;} public PushSubscription getSubscription(){return subscription;}
    public RoomNotification getNotification(){return notification;} public long getSubscriptionRevision(){return subscriptionRevision;}
    public int getAttempts(){return attempts;} public String getStatus(){return status;} public Instant getAvailableAt(){return availableAt;}
    public Instant getCreatedAt(){return createdAt;}
}
