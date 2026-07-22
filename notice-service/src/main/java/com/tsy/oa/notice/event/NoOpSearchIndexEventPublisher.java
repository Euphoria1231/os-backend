package com.tsy.oa.notice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NoOpSearchIndexEventPublisher implements SearchIndexEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpSearchIndexEventPublisher.class);

    @Override
    public void publish(SearchIndexEvent event) {
        log.debug("RocketMQ template is unavailable, skip search index event publishing, eventId={}", event.eventId());
    }
}
