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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void savesNoticeBatchWithOneBulkRequest() throws Exception {
        repository.saveNotices(List.of(
                new NoticeSearchDocument(
                        41L,
                        "放假通知",
                        "国庆节放假安排",
                        LocalDateTime.of(2026, 7, 22, 9, 0),
                        "PUBLISHED"
                ),
                new NoticeSearchDocument(
                        42L,
                        "会议通知",
                        "周五召开会议",
                        LocalDateTime.of(2026, 7, 22, 10, 0),
                        "PUBLISHED"
                )
        ));

        String request = server.bulkRequests().getFirst();
        String[] lines = request.split("\\n");
        assertThat(lines).hasSize(4);
        assertThat(objectMapper.readTree(lines[0]).path("index").path("_index").asText())
                .isEqualTo("oa-notices-v1");
        assertThat(objectMapper.readTree(lines[0]).path("index").path("_id").asText())
                .isEqualTo("notice-41");
        assertThat(objectMapper.readTree(lines[2]).path("index").path("_id").asText())
                .isEqualTo("notice-42");
        assertThat(lines[1]).contains("国庆节放假安排");
        assertThat(lines[3]).contains("周五召开会议");
    }

    @Test
    void rejectsBulkResponseContainingPartialFailure() {
        server.setBulkResponses("""
                {"errors":true,"items":[{"index":{"_id":"notice-41","status":400}}]}
                """);

        assertThatThrownBy(() -> repository.saveNotices(List.of(
                new NoticeSearchDocument(
                        41L,
                        "放假通知",
                        "国庆节放假安排",
                        LocalDateTime.of(2026, 7, 22, 9, 0),
                        "PUBLISHED"
                )
        )))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("failed items");
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
                2L,
                "LEAVE",
                "PENDING",
                "身体不适",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 8, 30)
        ));
        repository.saveApplication(new ApplicationSearchDocument(
                15L,
                3L,
                2L,
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
                2L,
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
