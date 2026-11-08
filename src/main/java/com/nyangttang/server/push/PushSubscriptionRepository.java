package com.nyangttang.server.push;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PushSubscription s where s.endpointHash=:hash")
    Optional<PushSubscription> lockByHash(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PushSubscription s where s.id=:id")
    Optional<PushSubscription> lockById(@Param("id") Long id);
    long countByUser_IdAndActiveTrue(Long userId);
    List<PushSubscription> findByUser_IdAndActiveTrueOrderByIdAsc(Long userId);
}
