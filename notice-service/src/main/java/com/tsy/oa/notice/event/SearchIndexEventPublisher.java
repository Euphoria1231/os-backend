package com.tsy.oa.notice.event;

public interface SearchIndexEventPublisher {

    void publish(SearchIndexEvent event);
}
