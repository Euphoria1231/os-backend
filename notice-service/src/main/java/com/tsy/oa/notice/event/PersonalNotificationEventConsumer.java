package com.tsy.oa.notice.event;

import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "oa.notification.events",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RocketMQMessageListener(
        topic = "${oa.notification.events.topic:oa-personal-notification-events}",
        consumerGroup = "${oa.notification.events.consumer-group:notice-personal-notification-consumer}"
)
public class PersonalNotificationEventConsumer
        implements RocketMQListener<PersonalNotificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            PersonalNotificationEventConsumer.class
    );

    private final PersonalNotificationEventHandler eventHandler;

    public PersonalNotificationEventConsumer(PersonalNotificationEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void onMessage(PersonalNotificationEvent event) {
        boolean created = eventHandler.handle(event);
        LOGGER.info(
                "Personal notification event processed eventId={} recipientEmployeeId={} created={}",
                event.eventId(),
                event.recipientEmployeeId(),
                created
        );
    }
}
