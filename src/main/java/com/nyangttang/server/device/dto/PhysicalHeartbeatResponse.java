package com.nyangttang.server.device.dto;

// Intentionally NO {data: ...} envelope: this is the firmware contract.
public record PhysicalHeartbeatResponse(String face, int landPercent, long remainMinutes,
                                        FaceEvent event) {
    public record FaceEvent(long id, String type) {}
}
