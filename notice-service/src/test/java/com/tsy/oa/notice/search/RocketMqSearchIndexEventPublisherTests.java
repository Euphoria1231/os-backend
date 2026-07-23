package com.tsy.oa.notice.search;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RocketMqSearchIndexEventPublisherTests {

    @Test
    void sendsEventSynchronouslyToConfiguredTopic() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        RocketMqSearchIndexEventPublisher publisher = new RocketMqSearchIndexEventPublisher(
                provider(rocketMQTemplate),
                "oa-search-index-events"
        );
        SearchIndexEvent event = event();

        publisher.publish(event);

        verify(rocketMQTemplate).syncSend("oa-search-index-events", event);
    }

    @Test
    void failsExplicitlyWhenRocketMqIsNotConfigured() {
        RocketMqSearchIndexEventPublisher publisher = new RocketMqSearchIndexEventPublisher(
                provider(null),
                "oa-search-index-events"
        );

        assertThrows(IllegalStateException.class, () -> publisher.publish(event()));
    }

    private SearchIndexEvent event() {
        return new SearchIndexEvent(
                "notice:1001:v:1",
                SearchIndexEvent.AggregateType.NOTICE,
                SearchIndexEvent.Operation.UPSERT,
                1001L,
                1L,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RocketMQTemplate> provider(RocketMQTemplate rocketMQTemplate) {
        ObjectProvider<RocketMQTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(rocketMQTemplate);
        return provider;
    }
}
