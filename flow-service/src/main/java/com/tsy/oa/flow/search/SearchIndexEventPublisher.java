package com.tsy.oa.flow.search;

@FunctionalInterface
public interface SearchIndexEventPublisher {

    void publish(SearchIndexEvent event);
}
