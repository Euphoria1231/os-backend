package com.tsy.oa.flow.event;

public interface SearchIndexEventPublisher {

    void publish(SearchIndexEvent event);
}
