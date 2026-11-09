package com.nyangttang.server.notification.repository;

import com.nyangttang.server.notification.entity.RoomNotification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoomNotificationRepository extends JpaRepository<RoomNotification, Long> {
    boolean existsByRoom_IdAndEventSeqAndActionIndex(Long roomId, Long eventSeq, Integer actionIndex);
    long countByRecipient_User_IdAndReadAtIsNull(Long userId);

    @Query("""
            select n from RoomNotification n join fetch n.room join fetch n.recipient
            where n.recipient.user.id = :userId
              and (:beforeId is null or n.id < :beforeId)
              and (:unreadOnly = false or n.readAt is null)
            order by n.id desc
            """)
    List<RoomNotification> findPage(@Param("userId") Long userId, @Param("beforeId") Long beforeId,
                                    @Param("unreadOnly") boolean unreadOnly, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from RoomNotification n where n.id = :id and n.recipient.user.id = :userId")
    Optional<RoomNotification> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
