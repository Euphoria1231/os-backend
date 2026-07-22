package com.tsy.oa.intelligence.search.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchIndexEventConsumerTests {

    @Test
    void delegatesRocketMqMessageToTheEventHandler() {
        AtomicReference<SearchIndexEvent> received = new AtomicReference<>();
        SearchIndexEventHandler handler = event -> {
            received.set(event);
            return SearchEventProcessingResult.PROCESSED;
        };
        SearchIndexEventConsumer consumer = new SearchIndexEventConsumer(handler);
        SearchIndexEvent event = event("event-1");

        consumer.onMessage(event);

        assertThat(received).hasValue(event);
    }

    @Test
    void propagatesProcessingFailureSoRocketMqCanRetry() {
        SearchIndexEventHandler handler = event -> {
            throw new IOException("Elasticsearch unavailable");
        };
        SearchIndexEventConsumer consumer = new SearchIndexEventConsumer(handler);

        assertThatThrownBy(() -> consumer.onMessage(event("event-2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event-2")
                .hasCauseInstanceOf(IOException.class);
    }

    private SearchIndexEvent event(String eventId) {
        return new SearchIndexEvent(
                eventId,
                SearchIndexEvent.AggregateType.NOTICE,
                SearchIndexEvent.Operation.DELETE,
                42L,
                1L,
                null
        );
    }
}
