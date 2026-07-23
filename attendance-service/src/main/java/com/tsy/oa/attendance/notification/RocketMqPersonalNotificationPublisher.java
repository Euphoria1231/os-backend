package com.tsy.oa.attendance.notification;

import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqPersonalNotificationPublisher implements PersonalNotificationPublisher {

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;
    private final String topic;

    public RocketMqPersonalNotificationPublisher(
            ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
            @Value("${oa.notification.events.topic:oa-personal-notification-events}") String topic
    ) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
        this.topic = topic;
    }

    @Override
    public void publish(PersonalNotificationEvent event) {
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            throw new IllegalStateException("RocketMQ is not configured for personal notifications");
        }
        rocketMQTemplate.syncSend(topic, event);
    }
}
