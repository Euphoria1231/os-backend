package com.tsy.oa.attendance.notification;

import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RocketMqPersonalNotificationPublisherTests {

    @Test
    void sendsEventSynchronouslyToConfiguredTopic() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        RocketMqPersonalNotificationPublisher publisher =
                new RocketMqPersonalNotificationPublisher(
                        provider(rocketMQTemplate),
                        "oa-personal-notification-events"
                );
        PersonalNotificationEvent event = event();

        publisher.publish(event);

        verify(rocketMQTemplate).syncSend("oa-personal-notification-events", event);
    }

    @Test
    void failsExplicitlyWhenRocketMqIsNotConfigured() {
        RocketMqPersonalNotificationPublisher publisher =
                new RocketMqPersonalNotificationPublisher(
                        provider(null),
                        "oa-personal-notification-events"
                );

        assertThrows(IllegalStateException.class, () -> publisher.publish(event()));
    }

    private PersonalNotificationEvent event() {
        return new PersonalNotificationEvent(
                "attendance:1001:late",
                10L,
                PersonalNotificationEvent.NotificationType.ATTENDANCE_ABNORMAL,
                "考勤异常提醒",
                "您于 2026-07-20T09:05 上班打卡，系统判定为迟到",
                PersonalNotificationEvent.RelatedBusinessType.ATTENDANCE_RECORD,
                1001L,
                LocalDateTime.of(2026, 7, 20, 9, 5)
        );
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RocketMQTemplate> provider(RocketMQTemplate rocketMQTemplate) {
        ObjectProvider<RocketMQTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(rocketMQTemplate);
        return provider;
    }
}
