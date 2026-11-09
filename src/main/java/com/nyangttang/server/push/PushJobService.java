package com.nyangttang.server.push;

import com.nyangttang.server.notification.dto.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;

@Service
public class PushJobService {
    private final PushDeliveryRepository deliveries;private final PushSubscriptionRepository subscriptions;
    private final JsonMapper json=JsonMapper.builder().build();
    public PushJobService(PushDeliveryRepository deliveries,PushSubscriptionRepository subscriptions){this.deliveries=deliveries;this.subscriptions=subscriptions;}
    public record Job(Long id,String leaseToken,String endpoint,String p256dh,String auth,byte[] payload,int ttl) {
        @Override public String toString(){return "PushJob[id="+id+"]";}
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Optional<Job> claim(Instant now){
        // Bound cancelled-row cleanup per tick; network delivery happens after this transaction returns.
        for(int i=0;i<20;i++){
            var d=deliveries.claimNext(now).orElse(null);if(d==null)return Optional.empty();
            var s=d.getSubscription();var n=d.getNotification();
            if(!s.isActive() || s.getRevision()!=d.getSubscriptionRevision() || n.getReadAt()!=null || d.expired(now)){
                d.finish("CANCELLED",null);continue;
            }
            if(d.getAttempts()>=5){d.finish("FAILED",null);continue;}
            var view=NotificationResponse.from(n);String token=UUID.randomUUID().toString();
            var body=Map.of("notificationId",view.id(),"roomId",view.roomId(),"title",view.title(),"body",view.message(),
                    "tag","nyangttang-"+view.id(),"url","/?roomId="+view.roomId()+"&notificationId="+view.id());
            d.claim(now,token);
            int ttl=(int)Math.min(900,Math.max(1,900-java.time.Duration.between(d.getCreatedAt(),now).getSeconds()));
            return Optional.of(new Job(d.getId(),token,s.getEndpoint(),s.getP256dh(),s.getAuthSecret(),json.writeValueAsBytes(body),ttl));
        }
        return Optional.empty();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void complete(Job job,PushTransport.Result result,Instant now){
        var d=deliveries.lockById(job.id()).orElse(null);
        if(d==null || !d.ownsLease(job.leaseToken()))return;
        var s=subscriptions.lockById(d.getSubscription().getId()).orElse(null);
        if(s==null || !s.isActive() || s.getRevision()!=d.getSubscriptionRevision()){d.finish("CANCELLED",result.status());return;}
        int code=result.status();
        if(code>=200 && code<300){d.finish("SENT",code);return;}
        if(code==404 || code==410){s.deactivate(now);d.finish("FAILED",code);return;}
        boolean retry=code==0 || code==408 || code==429 || code>=500;
        if(retry && d.getAttempts()<5 && !d.expired(now)){
            long backoff=new long[]{15,60,180,300,600}[Math.min(4,d.getAttempts()-1)];
            long delay=Math.max(backoff,Math.min(900,Math.max(0,result.retryAfterSeconds())));
            d.retry(now.plusSeconds(delay),code);
        }else d.finish("FAILED",code);
    }
    @Transactional public void cleanup(){deliveries.deleteFinishedBefore(Instant.now().minusSeconds(7*86400L));}
}
