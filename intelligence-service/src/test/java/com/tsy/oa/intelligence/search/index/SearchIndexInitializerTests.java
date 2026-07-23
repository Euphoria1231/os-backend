package com.tsy.oa.intelligence.search.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.repository.RestElasticsearchGateway;
import com.tsy.oa.intelligence.search.repository.ElasticsearchSearchIndexRepository;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.support.ElasticsearchStubServer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
                .contains("\"approverId\":{\"type\":\"long\"}")
                .contains("\"approverIds\":{\"type\":\"long\"}")
                .contains("\"sourceVersion\":{\"type\":\"long\"}");
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

    @Test
    void migratesLegacyApplicationMappingBeforeAttachingStableAliasesAndWritingApprover() throws Exception {
        server.seedIndexDefinition("oa-notices-v1", """
                {"mappings":{"dynamic":"strict","properties":{"noticeId":{"type":"long"},"title":{"type":"text"},"content":{"type":"text"},"publishedAt":{"type":"date"},"status":{"type":"keyword"}}}}
                """);
        server.seedIndexDefinition("oa-applications-v1", """
                {"mappings":{"dynamic":"strict","properties":{"applicationId":{"type":"long"},"applicantId":{"type":"long"},"type":{"type":"keyword"},"status":{"type":"keyword"},"reasonSummary":{"type":"text"},"submittedAt":{"type":"date"},"updatedAt":{"type":"date"}}}}
                """);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RestElasticsearchGateway gateway = new RestElasticsearchGateway(restClient, objectMapper);

        new SearchIndexInitializer(gateway, properties).initialize();

        assertThat(server.mappingRequests())
                .anyMatch(request -> request.startsWith("oa-applications-v1\n")
                        && request.contains("\"approverId\"")
                        && request.contains("\"approverIds\"")
                        && request.contains("\"sourceVersion\"")
                        && request.contains("\"long\""));
        assertThat(server.aliasTarget("oa-notices")).isEqualTo("oa-notices-v1");
        assertThat(server.aliasTarget("oa-applications")).isEqualTo("oa-applications-v1");

        ElasticsearchSearchIndexRepository repository = new ElasticsearchSearchIndexRepository(
                gateway, objectMapper, properties
        );
        repository.saveApplication(new ApplicationSearchDocument(
                15L, 3L, 2L, "LEAVE", "PENDING", "病假申请",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 8, 30)
        ));
        assertThat(server.documents()).containsKey("/oa-applications-v1/_doc/application-15");
    }

    @Test
    void migratesApplicationAccessMappingOnCurrentAliasTarget() throws Exception {
        server.seedIndexDefinition("oa-notices-v1", SearchIndexSchema.NOTICE_DEFINITION);
        server.seedIndexDefinition("oa-applications-v1", SearchIndexSchema.APPLICATION_DEFINITION);
        server.seedIndexDefinition("oa-applications-rebuild-legacy", """
                {"mappings":{"dynamic":"strict","properties":{"applicationId":{"type":"long"},"applicantId":{"type":"long"},"approverId":{"type":"long"},"type":{"type":"keyword"},"status":{"type":"keyword"},"reasonSummary":{"type":"text"},"submittedAt":{"type":"date"},"updatedAt":{"type":"date"}}}}
                """);
        server.attachAlias("oa-applications", "oa-applications-rebuild-legacy");

        new SearchIndexInitializer(
                new RestElasticsearchGateway(restClient, new ObjectMapper()),
                properties
        ).initialize();

        assertThat(server.mappingRequests())
                .anyMatch(request -> request.startsWith("oa-applications-rebuild-legacy\n")
                        && request.contains("\"approverIds\"")
                        && request.contains("\"sourceVersion\""));
        assertThat(server.aliasTarget("oa-applications"))
                .isEqualTo("oa-applications-rebuild-legacy");
    }

    @Test
    void keepsAliasesOnSuccessfulRebuildTargetsDuringRestart() throws Exception {
        server.seedIndexDefinition("oa-notices-v1", SearchIndexSchema.NOTICE_DEFINITION);
        server.seedIndexDefinition("oa-applications-v1", SearchIndexSchema.APPLICATION_DEFINITION);
        server.seedIndexDefinition("oa-notices-rebuild-current", SearchIndexSchema.NOTICE_DEFINITION);
        server.seedIndexDefinition("oa-applications-rebuild-current", SearchIndexSchema.APPLICATION_DEFINITION);
        server.attachAlias("oa-notices", "oa-notices-rebuild-current");
        server.attachAlias("oa-applications", "oa-applications-rebuild-current");
        SearchIndexInitializer initializer = new SearchIndexInitializer(
                new RestElasticsearchGateway(restClient, new ObjectMapper()),
                properties
        );

        initializer.initialize();

        assertThat(server.aliasTarget("oa-notices")).isEqualTo("oa-notices-rebuild-current");
        assertThat(server.aliasTarget("oa-applications")).isEqualTo("oa-applications-rebuild-current");
    }
}
