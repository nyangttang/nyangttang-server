package com.nyangttang.server.device.repository;

import com.nyangttang.server.device.entity.Device;
import com.nyangttang.server.device.entity.DeviceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByMember_Id(Long memberId);
    List<Device> findAllByMember_Room_IdAndSource(Long roomId, DeviceSource source);

    @Query("select distinct d.member.room.id from Device d where d.source = :source and d.member is not null")
    List<Long> findRoomIdsBySource(@Param("source") DeviceSource source);

    Optional<Device> findByDeviceCode(String deviceCode);
    List<Device> findAllByMember_Room_IdOrderByMember_SlotNoAsc(Long roomId);

    @Query("select distinct d.member.room.id from Device d where d.member is not null")
    List<Long> findConnectedRoomIds();

    // Scalar lookup avoids loading stale Device state before acquiring the room lock.
    @Query("select d.member.room.id from Device d where d.deviceCode = :code and d.source = :source")
    Optional<Long> findRoomIdByCode(@Param("code") String code, @Param("source") DeviceSource source);
}
