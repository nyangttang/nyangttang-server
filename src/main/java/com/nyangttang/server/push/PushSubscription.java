package com.nyangttang.server.push;

import com.nyangttang.server.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

@Entity @Table(name="push_subscriptions")
public class PushSubscription {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="endpoint",nullable=false,length=2048) private String endpoint;
    @Column(name="endpoint_hash",nullable=false,length=64,unique=true) private String endpointHash;
    @Column(name="p256dh",nullable=false,length=128) private String p256dh;
    @Column(name="auth_secret",nullable=false,length=32) private String authSecret;
    @Column(name="active",nullable=false) private boolean active=true;
    @Column(name="revision",nullable=false) private long revision=1;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="created_at",nullable=false,columnDefinition="DATETIME(3)") private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="updated_at",nullable=false,columnDefinition="DATETIME(3)") private Instant updatedAt;
    protected PushSubscription() {}
    public PushSubscription(User user,PushInput input,Instant now) {
        this.user=user;endpoint=input.endpoint();endpointHash=PushValidation.hash(endpoint);
        p256dh=input.keys().p256dh();authSecret=input.keys().auth();createdAt=now;updatedAt=now;
    }
    public void renew(PushInput input,Instant now) {
        if(!endpoint.equals(input.endpoint()))throw new IllegalArgumentException("endpoint 변경 불가");
        if(!active || !Objects.equals(p256dh,input.keys().p256dh()) || !Objects.equals(authSecret,input.keys().auth())) revision++;
        p256dh=input.keys().p256dh();authSecret=input.keys().auth();active=true;updatedAt=now;
    }
    public void deactivate(Instant now) {if(active){active=false;revision++;updatedAt=now;}}
    public Long getId(){return id;} public User getUser(){return user;} public String getEndpoint(){return endpoint;}
    public String getP256dh(){return p256dh;} public String getAuthSecret(){return authSecret;}
    public boolean isActive(){return active;} public long getRevision(){return revision;}
    public Instant getCreatedAt(){return createdAt;}
}
