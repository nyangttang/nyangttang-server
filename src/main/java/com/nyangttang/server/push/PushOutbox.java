package com.nyangttang.server.push;

import com.nyangttang.server.notification.entity.RoomNotification;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class PushOutbox {
    private final PushSubscriptionRepository subscriptions;private final PushDeliveryRepository deliveries;private final PushSettings settings;
    public PushOutbox(PushSubscriptionRepository subscriptions,PushDeliveryRepository deliveries,PushSettings settings){this.subscriptions=subscriptions;this.deliveries=deliveries;this.settings=settings;}
    // Persist only. No network I/O while the study transaction holds a room lock.
    @EventListener @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(RoomNotification notification){
        if(!settings.enabled())return;
        for(var s:subscriptions.findByUser_IdAndActiveTrueOrderByIdAsc(notification.getRecipient().getUser().getId()))
            deliveries.save(new PushDelivery(s,notification));
    }
}
