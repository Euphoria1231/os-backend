package com.tsy.oa.notice.message.event;

import java.time.LocalDateTime;

public record ApprovalCompletedEvent(
        String eventId,
        Long applicationId,
        String applicationType,
        Long applicantId,
        String result,
        LocalDateTime completedAt,
        String traceId
) {
}
