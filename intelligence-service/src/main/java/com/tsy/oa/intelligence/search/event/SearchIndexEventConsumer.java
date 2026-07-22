package com.tsy.oa.intelligence.search.event;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(
        prefix = "oa.search.events",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RocketMQMessageListener(
        topic = "${oa.search.events.topic:oa-search-index-events}",
        consumerGroup = "${oa.search.events.consumer-group:intelligence-search-index-consumer}"
)
public class SearchIndexEventConsumer implements RocketMQListener<SearchIndexEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexEventConsumer.class);

    private final SearchIndexEventHandler eventHandler;

    public SearchIndexEventConsumer(SearchIndexEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void onMessage(SearchIndexEvent event) {
        try {
            SearchEventProcessingResult result = eventHandler.process(event);
            LOGGER.info(
                    "Search index event processed eventId={} aggregateType={} aggregateId={} result={}",
                    event.eventId(),
                    event.aggregateType(),
                    event.aggregateId(),
                    result
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to process search index event [" + event.eventId() + "]",
                    exception
            );
        }
    }
}
