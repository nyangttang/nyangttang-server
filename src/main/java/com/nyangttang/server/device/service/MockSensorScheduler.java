package com.nyangttang.server.device.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class MockSensorScheduler {
    private static final Logger log = LoggerFactory.getLogger(MockSensorScheduler.class);
    private final MockSensorService service;
    private volatile boolean ready;

    public MockSensorScheduler(MockSensorService service) { this.service = service; }

    @EventListener(ApplicationReadyEvent.class)
    public void resetAfterRestart() {
        for (Long roomId : service.connectedRoomIds()) service.resetRoomAfterRestart(roomId);
        ready = true;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void tick() {
        if (!ready) return;
        for (Long roomId : service.connectedRoomIds()) {
            try {
                service.tickRoom(roomId);
            } catch (RuntimeException e) {
                log.error("기기 상태 갱신 실패: roomId={}", roomId, e);
            }
        }
    }
}
