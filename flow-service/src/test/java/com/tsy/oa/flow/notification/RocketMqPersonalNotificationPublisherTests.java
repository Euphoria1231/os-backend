package com.tsy.oa.flow.notification;

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
                "flow:1001:task:1",
                20L,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "新的审批待办",
                "请假申请 L202607240001 等待您审批",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                1001L,
                LocalDateTime.of(2026, 7, 24, 9, 30)
        );
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RocketMQTemplate> provider(RocketMQTemplate rocketMQTemplate) {
        ObjectProvider<RocketMQTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(rocketMQTemplate);
        return provider;
    }
}
