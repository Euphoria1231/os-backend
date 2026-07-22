package com.tsy.oa.intelligence.search.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.repository.RestElasticsearchGateway;
import com.tsy.oa.intelligence.search.support.ElasticsearchStubServer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchIndexInitializerTests {

    private ElasticsearchStubServer server;
    private RestClient restClient;
    private ElasticsearchSearchProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        server = new ElasticsearchStubServer();
        restClient = RestClient.builder(HttpHost.create(server.url())).build();
        properties = new ElasticsearchSearchProperties();
        properties.setNoticeIndex("oa-notices-v1");
        properties.setApplicationIndex("oa-applications-v1");
    }

    @AfterEach
    void tearDown() throws Exception {
        restClient.close();
        server.close();
    }

    @Test
    void createsNoticeAndApplicationIndexesWithIkMappings() {
        SearchIndexInitializer initializer = new SearchIndexInitializer(
                new RestElasticsearchGateway(restClient, new ObjectMapper()),
                properties
        );

        initializer.initialize();

        assertThat(server.indexDefinitions()).containsOnlyKeys("oa-notices-v1", "oa-applications-v1");
        assertThat(server.indexDefinitions().get("oa-notices-v1"))
                .contains("\"dynamic\":\"strict\"")
                .contains("\"analyzer\":\"ik_max_word\"")
                .contains("\"search_analyzer\":\"ik_smart\"")
                .contains("\"noticeId\":{\"type\":\"long\"}");
        assertThat(server.indexDefinitions().get("oa-applications-v1"))
                .contains("\"dynamic\":\"strict\"")
                .contains("\"analyzer\":\"ik_max_word\"")
                .contains("\"search_analyzer\":\"ik_smart\"")
                .contains("\"applicationId\":{\"type\":\"long\"}")
                .contains("\"approverId\":{\"type\":\"long\"}");
    }

    @Test
    void reportsClearErrorWhenIkAnalyzerIsUnavailable() {
        server.setAnalyzerAvailable(false);
        SearchIndexInitializer initializer = new SearchIndexInitializer(
                new RestElasticsearchGateway(restClient, new ObjectMapper()),
                properties
        );

        assertThatThrownBy(initializer::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("analysis-ik")
                .hasMessageContaining("7.13.0")
                .hasMessageContaining("ik_max_word");
        assertThat(server.indexDefinitions()).isEmpty();
    }
}
