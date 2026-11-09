package com.nyangttang.server.management;

import com.nyangttang.server.common.error.ApiException;
import com.nyangttang.server.device.entity.Device;
import com.nyangttang.server.device.repository.DeviceRepository;
import com.nyangttang.server.game.entity.*;
import com.nyangttang.server.game.repository.*;
import com.nyangttang.server.room.entity.*;
import com.nyangttang.server.room.repository.*;
import com.nyangttang.server.room.service.RoomSnapshotAssembler;
import com.nyangttang.server.study.entity.StudyEndReason;
import com.nyangttang.server.study.service.StudyAccountingService;
import com.nyangttang.server.replay.entity.RoomEvent;
import com.nyangttang.server.replay.repository.RoomEventRepository;
import com.nyangttang.server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class RoomManagementService {
    private final UserRepository users;private final RoomRepository rooms;private final RoomMemberRepository members;
    private final DeviceRepository devices;private final RoomRuntimeRepository maps;private final MemberRuntimeRepository runtimes;
    private final StudyAccountingService accounting;private final RoomSnapshotAssembler snapshots;
    private final RoomEventRepository events;private final ApplicationEventPublisher publisher;private final Clock clock;
    @Autowired
    public RoomManagementService(UserRepository users,RoomRepository rooms,RoomMemberRepository members,DeviceRepository devices,
            RoomRuntimeRepository maps,MemberRuntimeRepository runtimes,StudyAccountingService accounting,
            RoomSnapshotAssembler snapshots,RoomEventRepository events,ApplicationEventPublisher publisher){
        this(users,rooms,members,devices,maps,runtimes,accounting,snapshots,events,publisher,Clock.systemUTC());
    }
    RoomManagementService(UserRepository users,RoomRepository rooms,RoomMemberRepository members,DeviceRepository devices,
            RoomRuntimeRepository maps,MemberRuntimeRepository runtimes,StudyAccountingService accounting,
            RoomSnapshotAssembler snapshots,RoomEventRepository events,ApplicationEventPublisher publisher,Clock clock){
        this.users=users;this.rooms=rooms;this.members=members;this.devices=devices;this.maps=maps;this.runtimes=runtimes;
        this.accounting=accounting;this.snapshots=snapshots;this.events=events;this.publisher=publisher;this.clock=clock;
    }
    public record UnlinkResult(String roomId,boolean detached){}
    public record LeaveResult(String roomId,String memberId,boolean left,boolean roomClosed){}
    public UnlinkResult unlink(Long roomId,Long userId,String expectedCode){
        Room room=room(roomId);
        RoomMember member=members.findByUser_Id(userId).filter(m->m.getRoom().getId().equals(roomId))
                .orElseThrow(()->error(HttpStatus.FORBIDDEN,"FORBIDDEN","참여 중인 방에서만 해제할 수 있습니다."));
        Device current=devices.findByMember_Id(member.getId()).orElse(null);
        if(current==null)return new UnlinkResult(roomId.toString(),true);
        if(!current.getDeviceCode().equals(expectedCode))throw error(HttpStatus.CONFLICT,"DEVICE_CHANGED","기기가 변경되었습니다. 내 기기를 다시 조회해주세요.");
        MemberRuntime runtime=runtime(member);RoomRuntime map=map(roomId);Instant now=now(runtime);
        settle(room,map,runtime,current,now,StudyEndReason.DEVICE_UNLINKED);
        current.unbind();devices.saveAndFlush(current);
        record(room,map,member,now,"DEVICE_UNLINKED",List.of());
        return new UnlinkResult(roomId.toString(),true);
    }
    public LeaveResult leave(Long roomId,Long userId,Long expectedMemberId){
        // Same ordering as create/join: user -> room. Prevent concurrent join/leave for one user.
        users.findByIdForUpdate(userId).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"INVALID_TOKEN","사용자를 찾을 수 없습니다."));
        Room room=room(roomId);
        RoomMember member=members.findById(expectedMemberId)
                .filter(m->m.getUser().getId().equals(userId)&&m.getRoom().getId().equals(roomId))
                .orElseThrow(()->error(HttpStatus.FORBIDDEN,"FORBIDDEN","본인의 방 참가 정보가 아닙니다."));
        if(!member.isActive())return new LeaveResult(roomId.toString(),member.getId().toString(),true,room.getClosedAt()!=null);
        RoomRuntime map=map(roomId);MemberRuntime runtime=runtime(member);Instant now=now(runtime);
        Device device=devices.findByMember_Id(member.getId()).orElse(null);
        // Close the study interval BEFORE releasing land, preserving its earned-land summary.
        settle(room,map,runtime,device,now,StudyEndReason.ROOM_LEFT);
        if(device!=null){device.unbind();devices.saveAndFlush(device);}
        List<Integer> released=map.releaseMember(member.getId().toString());
        member.leave(now);members.flush();
        if(members.findAllByRoom_IdOrderBySlotNoAsc(roomId).isEmpty())room.close(now);
        record(room,map,member,now,"MEMBER_LEFT",released);
        return new LeaveResult(roomId.toString(),member.getId().toString(),true,room.getClosedAt()!=null);
    }
    private void settle(Room room,RoomRuntime map,MemberRuntime runtime,Device device,Instant now,StudyEndReason reason){
        if(device!=null)accounting.advance(room,map,runtime,device,now);
        accounting.endForManagement(room,map,runtime,now,reason);
    }
    private void record(Room room,RoomRuntime map,RoomMember member,Instant now,String type,List<Integer> released){
        map.markStateChanged(now);long seq=map.nextEventSequence();
        var action=new LinkedHashMap<String,Object>();action.put("type",type);action.put("memberId",member.getId().toString());
        action.put("effectiveAt",now.toString());
        if(type.equals("MEMBER_LEFT")){action.put("releasedCellIndexes",released);action.put("roomClosed",room.getClosedAt()!=null);}
        var snapshot=snapshots.publicSnapshot(room,map,now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate());
        var event=events.save(new RoomEvent(room,seq,now,map.getSnapshotVersion(),snapshot,List.of(action)));
        publisher.publishEvent(event);
    }
    private Room room(Long id){return rooms.findByIdForUpdate(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"NOT_FOUND","방을 찾을 수 없습니다."));}
    private RoomRuntime map(Long id){return maps.findById(id).orElseThrow(()->new IllegalStateException("지도 초기 데이터 누락"));}
    private MemberRuntime runtime(RoomMember member){return runtimes.findById(member.getId()).orElseThrow(()->new IllegalStateException("고양이 초기 데이터 누락"));}
    private Instant now(MemberRuntime runtime){Instant at=clock.instant().truncatedTo(ChronoUnit.MILLIS);return at.isBefore(runtime.getLastAccountedAt())?runtime.getLastAccountedAt():at;}
    private static ApiException error(HttpStatus status,String code,String text){return new ApiException(status,code,text);}
}
