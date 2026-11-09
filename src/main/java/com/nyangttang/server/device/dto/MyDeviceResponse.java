package com.nyangttang.server.device.dto;

import com.nyangttang.server.device.entity.DeviceSource;
import com.nyangttang.server.study.entity.StudyState;
import java.time.Instant;

public record MyDeviceResponse(String deviceCode, String memberId, DeviceSource source,
                               Instant lastSeenAt, boolean connected, StudyState state) {}
