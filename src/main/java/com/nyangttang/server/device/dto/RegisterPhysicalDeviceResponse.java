package com.nyangttang.server.device.dto;

public record RegisterPhysicalDeviceResponse(String deviceCode, String memberId,
                                             String source, String heartbeatPath) {}
