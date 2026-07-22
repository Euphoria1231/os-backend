package com.tsy.oa.flow.event;

import java.time.LocalDateTime;

public record SearchIndexEvent(
        String eventId,
        String resourceType,
        Long resourceId,
        String operation,
        LocalDateTime occurredAt,
        String traceId
) {
}
