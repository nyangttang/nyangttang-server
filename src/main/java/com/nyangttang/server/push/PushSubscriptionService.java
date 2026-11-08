package com.nyangttang.server.push;

import com.nyangttang.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
public class PushSubscriptionService {
    private final PushSubscriptionRepository subscriptions;
    private final UserRepository users;
    private final PushSettings settings;
    public PushSubscriptionService(PushSubscriptionRepository subscriptions,UserRepository users,PushSettings settings) {
        this.subscriptions=subscriptions;this.users=users;this.settings=settings;
    }
    public record SubscriptionView(String id,boolean active,Instant createdAt) {
        static SubscriptionView from(PushSubscription s){return new SubscriptionView(s.getId().toString(),s.isActive(),s.getCreatedAt());}
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SubscriptionView subscribe(Long userId,PushInput input) {
        if(!settings.enabled()) throw error(HttpStatus.SERVICE_UNAVAILABLE,"Push가 아직 활성화되지 않았습니다.");
        PushValidation.endpoint(input.endpoint());PushValidation.keys(input.keys().p256dh(),input.keys().auth());
        var user=users.findByIdForUpdate(userId).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"사용자를 찾을 수 없습니다."));
        var existing=subscriptions.lockByHash(PushValidation.hash(input.endpoint())).orElse(null);
        if(existing!=null && !existing.getUser().getId().equals(userId))
            throw error(HttpStatus.CONFLICT,"다른 계정의 브라우저 구독입니다. 브라우저 구독을 해제한 뒤 다시 등록해주세요.");
        if((existing==null || !existing.isActive()) && subscriptions.countByUser_IdAndActiveTrue(userId)>=5)
            throw error(HttpStatus.CONFLICT,"활성 브라우저는 최대 5개입니다.");
        Instant now=Instant.now();
        if(existing==null)existing=subscriptions.save(new PushSubscription(user,input,now));
        else existing.renew(input,now);
        return SubscriptionView.from(existing);
    }
    @Transactional(readOnly=true)
    public List<SubscriptionView> list(Long userId) {
        return subscriptions.findByUser_IdAndActiveTrueOrderByIdAsc(userId).stream().map(SubscriptionView::from).toList();
    }
    @Transactional
    public SubscriptionView unsubscribe(Long userId,Long id) {
        var s=subscriptions.lockById(id).filter(row->row.getUser().getId().equals(userId))
                .orElseThrow(()->error(HttpStatus.NOT_FOUND,"구독을 찾을 수 없습니다."));
        s.deactivate(Instant.now());return SubscriptionView.from(s);
    }
    static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
