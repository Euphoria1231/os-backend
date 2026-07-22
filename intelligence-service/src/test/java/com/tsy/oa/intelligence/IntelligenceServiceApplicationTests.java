package com.tsy.oa.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.support.ElasticsearchStubServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = IntelligenceServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntelligenceServiceApplicationTests {

    private static final ElasticsearchStubServer ELASTICSEARCH = startElasticsearch();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.search.elasticsearch.url", ELASTICSEARCH::url);
        registry.add("oa.search.sources.notice-url", ELASTICSEARCH::url);
        registry.add("oa.search.sources.application-url", ELASTICSEARCH::url);
    }

    @AfterAll
    static void stopElasticsearch() {
        ELASTICSEARCH.close();
    }

    @BeforeEach
    void resetElasticsearch() {
        ELASTICSEARCH.reset();
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/intelligence/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("intelligence-service"));
    }

    @Test
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("OA 搜索与智能平台 API"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/intelligence/health']").exists());
    }

    @Test
    void searchesChineseNoticeKeywordWithHighlightsAndPagination() throws Exception {
        ELASTICSEARCH.setSearchResponse("""
                {"hits":{"total":{"value":1,"relation":"eq"},"hits":[
                  {"_source":{"noticeId":7,"title":"会议通知","content":"周五召开全员会议","publishedAt":"2026-07-22T09:00:00","status":"PUBLISHED"},
                   "highlight":{"title":["<em>会议</em>通知"],"content":["周五召开全员<em>会议</em>"]}}
                ]}}
                """);

        mockMvc.perform(get("/api/intelligence/search/notices")
                        .header("X-Employee-Id", "10")
                        .header("X-Roles", "EMPLOYEE")
                        .param("keyword", "会议")
                        .param("page", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].noticeId").value(7))
                .andExpect(jsonPath("$.data.items[0].titleHighlight").value("<em>会议</em>通知"))
                .andExpect(jsonPath("$.data.items[0].contentHighlight").value("周五召开全员<em>会议</em>"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        JsonNode request = objectMapper.readTree(ELASTICSEARCH.lastSearchRequestBody());
        org.assertj.core.api.Assertions.assertThat(request.path("from").asInt()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(request.path("size").asInt()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(request.path("query").toString())
                .contains("会议", "title^2", "content", "PUBLISHED");
        org.assertj.core.api.Assertions.assertThat(request.path("highlight").toString())
                .contains("<em>", "title", "content");
    }

    @Test
    void filtersApplicationsByOwnerTypeAndStatusAndReturnsEmptyPage() throws Exception {
        ELASTICSEARCH.setSearchResponse(
                "{\"hits\":{\"total\":{\"value\":0,\"relation\":\"eq\"},\"hits\":[]}}"
        );

        mockMvc.perform(get("/api/intelligence/search/applications")
                        .header("X-Employee-Id", "10")
                        .header("X-Roles", "EMPLOYEE")
                        .param("keyword", "出差")
                        .param("type", "LEAVE")
                        .param("status", "PENDING")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        JsonNode employeeQuery = objectMapper.readTree(ELASTICSEARCH.lastSearchRequestBody());
        org.assertj.core.api.Assertions.assertThat(employeeQuery.path("from").asInt()).isZero();
        org.assertj.core.api.Assertions.assertThat(employeeQuery.path("size").asInt()).isEqualTo(20);
        JsonNode employeeFilters = employeeQuery.path("query").path("bool").path("filter");
        org.assertj.core.api.Assertions.assertThat(employeeFilters).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(employeeFilters.get(0).path("term")).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(employeeFilters.get(0).path("term").path("applicantId").asLong())
                .isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(employeeFilters.get(1).path("term").path("type").asText())
                .isEqualTo("LEAVE");
        org.assertj.core.api.Assertions.assertThat(employeeFilters.get(2).path("term").path("status").asText())
                .isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(employeeFilters.findValues("approverId")).isEmpty();
        org.assertj.core.api.Assertions.assertThat(employeeQuery.path("highlight").toString())
                .contains("<em>", "reasonSummary");

        mockMvc.perform(get("/api/intelligence/search/applications")
                        .header("X-Employee-Id", "2")
                        .header("X-Roles", "DEPARTMENT_MANAGER")
                        .param("keyword", "出差")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk());
        JsonNode managerQuery = objectMapper.readTree(ELASTICSEARCH.lastSearchRequestBody());
        JsonNode managerFilters = managerQuery.path("query").path("bool").path("filter");
        org.assertj.core.api.Assertions.assertThat(managerFilters).hasSize(1);
        JsonNode managerOwnerScope = managerFilters.get(0).path("bool");
        JsonNode managerOwnerTerms = managerOwnerScope.path("should");
        org.assertj.core.api.Assertions.assertThat(managerOwnerTerms).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(managerOwnerTerms.get(0).path("term")).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(managerOwnerTerms.get(0).path("term").path("applicantId").asLong())
                .isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(managerOwnerTerms.get(1).path("term")).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(managerOwnerTerms.get(1).path("term").path("approverId").asLong())
                .isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(managerOwnerScope.path("minimum_should_match").asInt())
                .isEqualTo(1);

        mockMvc.perform(get("/api/intelligence/search/applications")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN")
                        .param("keyword", "")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk());
        JsonNode administratorQuery = objectMapper.readTree(ELASTICSEARCH.lastSearchRequestBody());
        JsonNode administratorFilters = administratorQuery.path("query").path("bool").path("filter");
        org.assertj.core.api.Assertions.assertThat(administratorFilters.isArray()).isTrue();
        org.assertj.core.api.Assertions.assertThat(administratorFilters).isEmpty();
    }

    @Test
    void rejectsSearchPageBeyondElasticsearchResultWindow() throws Exception {
        mockMvc.perform(get("/api/intelligence/search/notices")
                        .param("keyword", "会议")
                        .param("page", "101")
                        .param("pageSize", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void rejectsIndexRebuildForNonAdministratorAndExposesIndexHealth() throws Exception {
        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "10")
                        .header("X-Roles", "EMPLOYEE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/intelligence/search/indexes/health")
                        .header("X-Employee-Id", "10")
                        .header("X-Roles", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeIndex.name").value("oa-notices"))
                .andExpect(jsonPath("$.data.applicationIndex.name").value("oa-applications"))
                .andExpect(jsonPath("$.data.noticeIndex.schemaReady").value(false))
                .andExpect(jsonPath("$.data.applicationIndex.schemaReady").value(false))
                .andExpect(jsonPath("$.data.noticeRebuild.status").value("IDLE"))
                .andExpect(jsonPath("$.data.applicationRebuild.status").value("IDLE"));
    }

    @Test
    void rebuildsIndexesFromPagedSourcesWithObservableProgress() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.setNoticeSourceResponses(
                """
                {"code":0,"message":"success","data":{"items":[
                  {"id":1,"title":"第一条公告","content":"公告正文一","status":"PUBLISHED","publishedAt":"2026-07-22T09:00:00"}
                ],"total":2,"page":1,"pageSize":100,"hasNext":true}}
                """,
                """
                {"code":0,"message":"success","data":{"items":[
                  {"id":2,"title":"第二条公告","content":"公告正文二","status":"PUBLISHED","publishedAt":"2026-07-22T10:00:00"}
                ],"total":2,"page":2,"pageSize":100,"hasNext":false}}
                """
        );
        ELASTICSEARCH.setApplicationSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":3,"applicantId":10,"approverId":2,"applicationType":"LEAVE","status":"APPROVED","reason":"家庭事务","createdAt":"2026-07-22T08:00:00","updatedAt":"2026-07-22T11:00:00"}
                ],"total":1,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ELASTICSEARCH.seedDocument(
                "oa-notices-v1", "notice-999",
                "{\"noticeId\":999,\"status\":\"PUBLISHED\"}"
        );

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.processed").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.currentPage").value(2));

        mockMvc.perform(post("/api/intelligence/search/indexes/applications/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.processed").value(1));

        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.sourceRequests())
                .anyMatch(request -> request.contains("page=1") && request.contains("pageSize=100"))
                .anyMatch(request -> request.contains("page=2") && request.contains("pageSize=100"));
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.bulkRequests()).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.bulkRequests().get(2))
                .contains("\"approverId\":2");
        String noticeTarget = ELASTICSEARCH.aliasTarget("oa-notices");
        String applicationTarget = ELASTICSEARCH.aliasTarget("oa-applications");
        org.assertj.core.api.Assertions.assertThat(noticeTarget)
                .startsWith("oa-notices-rebuild-")
                .isNotEqualTo("oa-notices-v1");
        org.assertj.core.api.Assertions.assertThat(applicationTarget)
                .startsWith("oa-applications-rebuild-")
                .isNotEqualTo("oa-applications-v1");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.documents())
                .containsKey("/oa-notices-v1/_doc/notice-999")
                .containsKeys(
                        "/" + noticeTarget + "/_doc/notice-1",
                        "/" + noticeTarget + "/_doc/notice-2",
                        "/" + applicationTarget + "/_doc/application-3"
                )
                .doesNotContainKey("/" + noticeTarget + "/_doc/notice-999");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.deleteByQueryRequests()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.refreshRequests())
                .contains("/" + noticeTarget + "/_refresh", "/" + applicationTarget + "/_refresh");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasRequests()).hasSize(2);
    }

    @Test
    void marksRebuildFailedWhenSourcePaginationContractIsInvalid() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.seedDocument("oa-notices-v1", "notice-999", "{\"noticeId\":999}");
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[],"total":0,"page":2,"pageSize":100,"hasNext":false}}
                """);

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(get("/api/intelligence/search/indexes/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeRebuild.status").value("FAILED"));
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasTarget("oa-notices"))
                .isEqualTo("oa-notices-v1");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.documents())
                .containsKey("/oa-notices-v1/_doc/notice-999");
    }

    @Test
    void marksRebuildFailedWhenFinalProcessedCountDiffersFromTotal() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":1,"title":"公告","content":"正文","status":"PUBLISHED","publishedAt":"2026-07-22T09:00:00"}
                ],"total":2,"page":1,"pageSize":100,"hasNext":false}}
                """);

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(get("/api/intelligence/search/indexes/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeRebuild.status").value("FAILED"));
    }

    @Test
    void marksRebuildFailedWhenBulkContainsPartialFailure() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.seedDocument("oa-notices-v1", "notice-999", "{\"noticeId\":999}");
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":1,"title":"公告","content":"正文","status":"PUBLISHED","publishedAt":"2026-07-22T09:00:00"}
                ],"total":1,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ELASTICSEARCH.setBulkResponses("""
                {"errors":true,"items":[{"index":{"_id":"notice-1","status":400,"error":{"type":"mapper_parsing_exception"}}}]}
                """);

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(get("/api/intelligence/search/indexes/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeRebuild.status").value("FAILED"));
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.refreshRequests()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasTarget("oa-notices"))
                .isEqualTo("oa-notices-v1");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.documents())
                .containsKey("/oa-notices-v1/_doc/notice-999");
    }

    @Test
    void keepsServingAliasWhenStagingRefreshHasPartialFailure() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.seedDocument("oa-notices-v1", "notice-999", "{\"noticeId\":999}");
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":1,"title":"公告","content":"正文","status":"PUBLISHED","publishedAt":"2026-07-22T09:00:00"}
                ],"total":1,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ELASTICSEARCH.setRefreshResponses("{\"_shards\":{\"total\":1,\"successful\":0,\"failed\":1}}");

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasTarget("oa-notices"))
                .isEqualTo("oa-notices-v1");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.documents())
                .containsKey("/oa-notices-v1/_doc/notice-999");
        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasRequests()).isEmpty();
    }

    @Test
    void rejectsUnacknowledgedAliasSwitchAndKeepsServingTarget() throws Exception {
        seedServingIndexes();
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[],"total":0,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ELASTICSEARCH.setAliasResponses("{\"acknowledged\":false}");

        mockMvc.perform(post("/api/intelligence/search/indexes/notices/rebuild")
                        .header("X-Employee-Id", "1")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(ELASTICSEARCH.aliasTarget("oa-notices"))
                .isEqualTo("oa-notices-v1");
    }

    private void seedServingIndexes() throws Exception {
        ELASTICSEARCH.seedIndexDefinition("oa-notices-v1", """
                {"mappings":{"dynamic":"strict","properties":{"noticeId":{"type":"long"},"title":{"type":"text"},"content":{"type":"text"},"publishedAt":{"type":"date"},"status":{"type":"keyword"}}}}
                """);
        ELASTICSEARCH.seedIndexDefinition("oa-applications-v1", """
                {"mappings":{"dynamic":"strict","properties":{"applicationId":{"type":"long"},"applicantId":{"type":"long"},"approverId":{"type":"long"},"type":{"type":"keyword"},"status":{"type":"keyword"},"reasonSummary":{"type":"text"},"submittedAt":{"type":"date"},"updatedAt":{"type":"date"}}}}
                """);
        ELASTICSEARCH.attachAlias("oa-notices", "oa-notices-v1");
        ELASTICSEARCH.attachAlias("oa-applications", "oa-applications-v1");
    }

    private static ElasticsearchStubServer startElasticsearch() {
        try {
            return new ElasticsearchStubServer();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start Elasticsearch test server", exception);
        }
    }
}
