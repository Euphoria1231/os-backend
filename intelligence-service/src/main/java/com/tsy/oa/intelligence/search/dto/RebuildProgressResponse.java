package com.tsy.oa.intelligence.search.dto;

import java.time.LocalDateTime;

public record RebuildProgressResponse(
        String indexType,
        String status,
        long processed,
        long total,
        int currentPage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String message
) {
    public static RebuildProgressResponse idle(String indexType) {
        return new RebuildProgressResponse(indexType, "IDLE", 0, 0, 0, null, null, null);
    }
}
