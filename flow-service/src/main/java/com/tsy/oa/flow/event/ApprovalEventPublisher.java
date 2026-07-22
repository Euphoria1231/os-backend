package com.tsy.oa.flow.event;

public interface ApprovalEventPublisher {

    void publish(ApprovalCompletedEvent event);
}
