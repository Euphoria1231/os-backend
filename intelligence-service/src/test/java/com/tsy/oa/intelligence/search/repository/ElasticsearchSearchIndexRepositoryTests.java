package com.tsy.oa.intelligence.search.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.support.ElasticsearchStubServer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchSearchIndexRepositoryTests {

    private ElasticsearchStubServer server;
    private RestClient restClient;
    private ObjectMapper objectMapper;
    private ElasticsearchSearchIndexRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        server = new ElasticsearchStubServer();
        restClient = RestClient.builder(HttpHost.create(server.url())).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        ElasticsearchSearchProperties properties = new ElasticsearchSearchProperties();
        properties.setNoticeIndex("oa-notices-v1");
        properties.setApplicationIndex("oa-applications-v1");
        repository = new ElasticsearchSearchIndexRepository(
                new RestElasticsearchGateway(restClient, objectMapper),
                objectMapper,
                properties
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        restClient.close();
        server.close();
    }

    @Test
    void repeatedNoticeWritesReplaceTheSameDocument() throws Exception {
        repository.saveNotice(new NoticeSearchDocument(
                42L,
                "放假通知",
                "原始正文",
                LocalDateTime.of(2026, 7, 22, 9, 0),
                "PUBLISHED"
        ));
        repository.saveNotice(new NoticeSearchDocument(
                42L,
                "放假通知（更新）",
                "更新后的正文",
                LocalDateTime.of(2026, 7, 22, 10, 0),
                "PUBLISHED"
        ));

        assertThat(server.documents()).hasSize(1);
        String source = server.documents().get("/oa-notices-v1/_doc/notice-42");
        JsonNode document = objectMapper.readTree(source);
        assertThat(document.path("title").asText()).isEqualTo("放假通知（更新）");
        assertThat(document.path("content").asText()).isEqualTo("更新后的正文");
    }

    @Test
    void deletesNoticeByDeterministicDocumentId() throws Exception {
        repository.saveNotice(new NoticeSearchDocument(
                7L,
                "会议通知",
                "周五召开会议",
                LocalDateTime.of(2026, 7, 22, 11, 0),
                "PUBLISHED"
        ));

        repository.deleteNotice(7L);

        assertThat(server.documents()).isEmpty();
    }

    @Test
    void repeatedApplicationWritesReplaceTheSameDocument() throws Exception {
        repository.saveApplication(new ApplicationSearchDocument(
                15L,
                3L,
                "LEAVE",
                "PENDING",
                "身体不适",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 8, 30)
        ));
        repository.saveApplication(new ApplicationSearchDocument(
                15L,
                3L,
                "LEAVE",
                "APPROVED",
                "身体不适，需要休息",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 12, 0)
        ));

        assertThat(server.documents()).hasSize(1);
        String source = server.documents().get("/oa-applications-v1/_doc/application-15");
        JsonNode document = objectMapper.readTree(source);
        assertThat(document.path("status").asText()).isEqualTo("APPROVED");
        assertThat(document.path("reasonSummary").asText()).isEqualTo("身体不适，需要休息");
    }

    @Test
    void deletesApplicationByDeterministicDocumentId() throws Exception {
        repository.saveApplication(new ApplicationSearchDocument(
                18L,
                5L,
                "OVERTIME",
                "PENDING",
                "项目上线",
                LocalDateTime.of(2026, 7, 22, 13, 0),
                LocalDateTime.of(2026, 7, 22, 13, 0)
        ));

        repository.deleteApplication(18L);

        assertThat(server.documents()).isEmpty();
    }
}
