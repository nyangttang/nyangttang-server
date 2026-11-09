package com.nyangttang.server.replay.dto;

public record TerritorySummary(long creditedMs, long gainedCount, long lostCount,
                               int startOwnedCellCount, int endOwnedCellCount,
                               int netOwnedCellChange) {}
