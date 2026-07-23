package com.tsy.oa.notice.event;

import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonalNotificationEventConsumerTests {

    @Test
    void delegatesRocketMqMessageToEventHandler() {
        AtomicReference<PersonalNotificationEvent> received = new AtomicReference<>();
        PersonalNotificationEventHandler handler = event -> {
            received.set(event);
            return true;
        };
        PersonalNotificationEventConsumer consumer = new PersonalNotificationEventConsumer(handler);
        PersonalNotificationEvent event = event("event-1");

        consumer.onMessage(event);

        assertEquals(event, received.get());
    }

    @Test
    void propagatesProcessingFailureSoRocketMqCanRetry() {
        PersonalNotificationEventHandler handler = event -> {
            throw new IllegalStateException("database unavailable");
        };
        PersonalNotificationEventConsumer consumer = new PersonalNotificationEventConsumer(handler);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.onMessage(event("event-2"))
        );

        assertEquals("database unavailable", exception.getMessage());
    }

    private PersonalNotificationEvent event(String eventId) {
        return new PersonalNotificationEvent(
                eventId,
                20L,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "新的审批待办",
                "申请 L202607240001 等待处理",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                1001L,
                LocalDateTime.of(2026, 7, 24, 9, 30)
        );
    }
}
