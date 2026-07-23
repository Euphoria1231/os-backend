package com.tsy.oa.notice.search;

@FunctionalInterface
public interface SearchIndexEventPublisher {

    void publish(SearchIndexEvent event);
}
