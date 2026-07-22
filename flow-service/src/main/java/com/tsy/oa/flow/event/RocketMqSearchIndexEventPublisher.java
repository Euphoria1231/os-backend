package com.tsy.oa.flow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Component
public class RocketMqSearchIndexEventPublisher implements SearchIndexEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqSearchIndexEventPublisher.class);

    private final ObjectProvider<Object> rocketMqTemplateProvider;
    private final SearchIndexEventPublisher fallbackPublisher = new NoOpSearchIndexEventPublisher();
    private final String topic;

    public RocketMqSearchIndexEventPublisher(
            @Qualifier("rocketMQTemplate") ObjectProvider<Object> rocketMqTemplateProvider,
            @Value("${oa.flow.search-index-events.topic:oa-search-index-events}") String topic
    ) {
        this.rocketMqTemplateProvider = rocketMqTemplateProvider;
        this.topic = topic;
    }

    @Override
    public void publish(SearchIndexEvent event) {
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
            throw new IllegalStateException("RocketMQTemplate convertAndSend(String, Object) method is unavailable");
        }
        ReflectionUtils.makeAccessible(convertAndSend);
        ReflectionUtils.invokeMethod(convertAndSend, rocketMqTemplate, topic, event);
        log.info("Published flow search index event, eventId={}, topic={}", event.eventId(), topic);
    }
}
