package com.nyangttang.server.replay.repository;

import java.util.List;
import java.util.Map;

public record RoomActionSlice(Long seq, List<Map<String, Object>> actions) {}
