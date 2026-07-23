package com.tsy.oa.flow.search;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqSearchIndexEventPublisher implements SearchIndexEventPublisher {

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;
    private final String topic;

    public RocketMqSearchIndexEventPublisher(
            ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
            @Value("${oa.search.events.topic:oa-search-index-events}") String topic
    ) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
        this.topic = topic;
    }

    @Override
    public void publish(SearchIndexEvent event) {
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            throw new IllegalStateException("RocketMQ is not configured for search index events");
        }
        rocketMQTemplate.syncSend(topic, event);
    }
}
