package com.tsy.oa.common.notification;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonalNotificationEventTests {

    @Test
    void keepsStableEventIdentityAndBusinessTarget() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 24, 9, 30);

        PersonalNotificationEvent event = new PersonalNotificationEvent(
                "flow-submit-1001",
                20L,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "新的审批待办",
                "申请 L202607240001 等待处理",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                1001L,
                occurredAt
        );

        assertEquals("flow-submit-1001", event.eventId());
        assertEquals(20L, event.recipientEmployeeId());
        assertEquals(1001L, event.relatedBusinessId());
        assertEquals(occurredAt, event.occurredAt());
    }

    @Test
    void rejectsInvalidIdentityAndDisplayContent() {
        assertThrows(IllegalArgumentException.class, () -> event("", 20L, "标题", "内容"));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", 0L, "标题", "内容"));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", 20L, " ", "内容"));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", 20L, "标题", " "));
        assertThrows(IllegalArgumentException.class, () -> new PersonalNotificationEvent(
                "event-1",
                20L,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "标题",
                "内容",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                0L,
                LocalDateTime.now()
        ));
    }

    private PersonalNotificationEvent event(
            String eventId,
            long recipientEmployeeId,
            String title,
            String content
    ) {
        return new PersonalNotificationEvent(
                eventId,
                recipientEmployeeId,
                PersonalNotificationEvent.NotificationType.APPLICATION_APPROVED,
                title,
                content,
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                1001L,
                LocalDateTime.now()
        );
    }
}
