package com.nyangttang.server.push;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PushDeliveryRepository extends JpaRepository<PushDelivery,Long> {
    @Query(value="SELECT * FROM push_deliveries WHERE status IN ('PENDING','IN_FLIGHT') AND available_at<=:now ORDER BY available_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",nativeQuery=true)
    Optional<PushDelivery> claimNext(@Param("now") Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from PushDelivery d where d.id=:id")
    Optional<PushDelivery> lockById(@Param("id") Long id);
    @Modifying @Query("delete from PushDelivery d where d.createdAt<:before and d.status in ('SENT','FAILED','CANCELLED')")
    int deleteFinishedBefore(@Param("before") Instant before);
}
