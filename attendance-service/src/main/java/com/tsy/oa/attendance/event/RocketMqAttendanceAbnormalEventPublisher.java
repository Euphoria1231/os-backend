package com.tsy.oa.attendance.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Component
public class RocketMqAttendanceAbnormalEventPublisher
        implements AttendanceAbnormalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(
            RocketMqAttendanceAbnormalEventPublisher.class
    );

    private final ObjectProvider<Object> rocketMqTemplateProvider;
    private final AttendanceAbnormalEventPublisher fallbackPublisher =
            new NoOpAttendanceAbnormalEventPublisher();
    private final String topic;

    public RocketMqAttendanceAbnormalEventPublisher(
            @Qualifier("rocketMQTemplate") ObjectProvider<Object> rocketMqTemplateProvider,
            @Value("${oa.attendance.abnormal-events.topic:oa-attendance-abnormal-events}") String topic
    ) {
        this.rocketMqTemplateProvider = rocketMqTemplateProvider;
        this.topic = topic;
    }

    @Override
    public void publish(AttendanceAbnormalEvent event) {
        Object rocketMqTemplate = rocketMqTemplateProvider.getIfAvailable();
        if (rocketMqTemplate == null) {
            fallbackPublisher.publish(event);
            return;
        }
        Method convertAndSend = ReflectionUtils.findMethod(
                rocketMqTemplate.getClass(),
                "convertAndSend",
                String.class,
                Object.class
        );
        if (convertAndSend == null) {
            throw new IllegalStateException(
                    "RocketMQTemplate convertAndSend(String, Object) method is unavailable"
            );
        }
        ReflectionUtils.makeAccessible(convertAndSend);
        ReflectionUtils.invokeMethod(convertAndSend, rocketMqTemplate, topic, event);
        log.info(
                "Published attendance abnormal event, eventId={}, topic={}",
                event.eventId(),
                topic
        );
    }
}
