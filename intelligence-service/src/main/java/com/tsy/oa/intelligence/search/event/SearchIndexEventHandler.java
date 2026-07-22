package com.tsy.oa.intelligence.search.event;

import java.io.IOException;

@FunctionalInterface
public interface SearchIndexEventHandler {

    SearchEventProcessingResult process(SearchIndexEvent event) throws IOException;
}
