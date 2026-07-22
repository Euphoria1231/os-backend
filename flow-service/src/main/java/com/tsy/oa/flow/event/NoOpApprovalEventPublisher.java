package com.tsy.oa.flow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NoOpApprovalEventPublisher implements ApprovalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpApprovalEventPublisher.class);

    @Override
    public void publish(ApprovalCompletedEvent event) {
        log.debug("RocketMQ template is unavailable, skip approval event publishing, eventId={}", event.eventId());
    }
}
