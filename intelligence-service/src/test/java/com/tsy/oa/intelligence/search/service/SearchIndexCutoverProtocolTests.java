package com.tsy.oa.intelligence.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.IntelligenceServiceApplication;
import com.tsy.oa.intelligence.search.event.SearchEventProcessingResult;
import com.tsy.oa.intelligence.search.event.SearchIndexEvent;
import com.tsy.oa.intelligence.search.event.SearchIndexEventProcessor;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexCutoverBarrierMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexEventSequenceMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexReplayDelta;
import com.tsy.oa.intelligence.search.event.source.SearchDocumentSourceGateway;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.support.ElasticsearchStubServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = IntelligenceServiceApplication.class)
@Import(SearchIndexCutoverProtocolTests.TestBeans.class)
class SearchIndexCutoverProtocolTests {

    private static final ElasticsearchStubServer ELASTICSEARCH = startElasticsearch();

    @Autowired
    private SearchIndexAdministrationService administrationService;

    @Autowired
    private SearchIndexEventProcessor eventProcessor;

    @Autowired
    private SearchIndexCutoverCoordinator cutoverCoordinator;

    @Autowired
    private SearchIndexCutoverFinalizer cutoverFinalizer;

    @Autowired
    private SearchIndexCutoverBarrierMapper barrierMapper;

    @Autowired
    private SearchIndexEventSequenceMapper sequenceMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private StubSearchDocumentSourceGateway sourceGateway;

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
    void setUp() throws Exception {
        ELASTICSEARCH.reset();
        sourceGateway.clear();
        jdbcTemplate.update("DELETE FROM search_index_event_sequence");
        jdbcTemplate.update("DELETE FROM search_index_event_record");
        jdbcTemplate.update("DELETE FROM search_index_aggregate_state");
        jdbcTemplate.update("DELETE FROM search_index_cutover_barrier");
        seedServingIndexes();
    }

    @Test
    void replaysNoticeDeleteProcessedAfterSnapshotBeforeAliasCutover() throws Exception {
        ELASTICSEARCH.setNoticeSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":7,"title":"已发布公告","content":"公告正文","status":"PUBLISHED","publishedAt":"2026-07-22T09:00:00"}
                ],"total":1,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ELASTICSEARCH.seedDocument(
                "oa-notices-v1", "notice-7",
                "{\"noticeId\":7,\"title\":\"已发布公告\",\"content\":\"公告正文\",\"publishedAt\":\"2026-07-22T09:00:00\",\"status\":\"PUBLISHED\"}"
        );
        ELASTICSEARCH.pauseNextBulkResponseAfterApply();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> rebuild = executor.submit(administrationService::rebuildNotices);
            assertThat(ELASTICSEARCH.awaitPausedBulkApplied()).isTrue();

            SearchIndexEvent deleteEvent = new SearchIndexEvent(
                    "notice-delete-during-rebuild",
                    SearchIndexEvent.AggregateType.NOTICE,
                    SearchIndexEvent.Operation.DELETE,
                    7L,
                    1L,
                    null
            );
            assertThat(eventProcessor.process(deleteEvent))
                    .isEqualTo(SearchEventProcessingResult.PROCESSED);

            ELASTICSEARCH.releasePausedBulkResponse();
            rebuild.get(5, TimeUnit.SECONDS);
        } finally {
            ELASTICSEARCH.releasePausedBulkResponse();
            executor.shutdownNow();
        }

        String activeTarget = ELASTICSEARCH.aliasTarget("oa-notices");
        assertThat(ELASTICSEARCH.documents())
                .doesNotContainKey("/" + activeTarget + "/_doc/notice-7");
    }

    @Test
    void replaysCanonicalApplicationUpdateProcessedAfterSnapshotBeforeAliasCutover() throws Exception {
        ELASTICSEARCH.setApplicationSourceResponses("""
                {"code":0,"message":"success","data":{"items":[
                  {"id":15,"applicantId":3,"approverId":2,"applicationType":"LEAVE","status":"PENDING","reason":"病假申请","createdAt":"2026-07-22T08:30:00","updatedAt":"2026-07-22T08:30:00"}
                ],"total":1,"page":1,"pageSize":100,"hasNext":false}}
                """);
        ApplicationSearchDocument approved = new ApplicationSearchDocument(
                15L, 3L, 2L, "LEAVE", "APPROVED", "病假申请",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 12, 0)
        );
        sourceGateway.applications.put(15L, approved);
        ELASTICSEARCH.seedDocument(
                "oa-applications-v1", "application-15",
                objectMapper.writeValueAsString(approved)
        );
        ELASTICSEARCH.pauseNextBulkResponseAfterApply();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> rebuild = executor.submit(administrationService::rebuildApplications);
            assertThat(ELASTICSEARCH.awaitPausedBulkApplied()).isTrue();

            SearchIndexEvent updateEvent = new SearchIndexEvent(
                    "application-update-during-rebuild",
                    SearchIndexEvent.AggregateType.APPLICATION,
                    SearchIndexEvent.Operation.UPSERT,
                    15L,
                    1L,
                    objectMapper.valueToTree(approved)
            );
            assertThat(eventProcessor.process(updateEvent))
                    .isEqualTo(SearchEventProcessingResult.PROCESSED);

            ELASTICSEARCH.releasePausedBulkResponse();
            rebuild.get(5, TimeUnit.SECONDS);
        } finally {
            ELASTICSEARCH.releasePausedBulkResponse();
            executor.shutdownNow();
        }

        String activeTarget = ELASTICSEARCH.aliasTarget("oa-applications");
        String activeDocument = ELASTICSEARCH.documents()
                .get("/" + activeTarget + "/_doc/application-15");
        assertThat(objectMapper.readTree(activeDocument).path("status").asText())
                .isEqualTo("APPROVED");
    }

    @Test
    void databaseBarrierSerializesDifferentCoordinatorInstances() throws Exception {
        SearchIndexCutoverCoordinator firstCoordinator = new SearchIndexCutoverCoordinator(
                barrierMapper, sequenceMapper
        );
        SearchIndexCutoverCoordinator secondCoordinator = new SearchIndexCutoverCoordinator(
                barrierMapper, sequenceMapper
        );
        TransactionTemplate firstTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate secondTransaction = new TransactionTemplate(transactionManager);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> firstTransaction.executeWithoutResult(status -> {
                firstCoordinator.lock(SearchIndexEvent.AggregateType.NOTICE);
                firstLocked.countDown();
                await(releaseFirst);
            }));
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                secondTransaction.executeWithoutResult(status ->
                        secondCoordinator.lock(SearchIndexEvent.AggregateType.NOTICE)
                );
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void replaysOneHundredAndOneDeltasInStrictPagesAndDeletesMissingUpsertSource() throws Exception {
        String stagingIndex = "oa-notices-rebuild-pagination";
        ELASTICSEARCH.seedIndexDefinition(stagingIndex, """
                {"mappings":{"dynamic":"strict","properties":{"noticeId":{"type":"long"},"title":{"type":"text"},"content":{"type":"text"},"publishedAt":{"type":"date"},"status":{"type":"keyword"}}}}
                """);
        long watermark = cutoverCoordinator.captureWatermark(SearchIndexEvent.AggregateType.NOTICE);

        for (long aggregateId = 1; aggregateId <= 101; aggregateId++) {
            String eventId = "notice-replay-" + aggregateId;
            String operation = aggregateId == 101 ? "UPSERT" : "DELETE";
            jdbcTemplate.update(
                    "INSERT INTO search_index_event_record "
                            + "(event_id, aggregate_type, aggregate_id, event_version, operation, "
                            + "processing_status, processed_at) VALUES (?, 'NOTICE', ?, 1, ?, "
                            + "'PROCESSED', CURRENT_TIMESTAMP)",
                    eventId,
                    aggregateId,
                    operation
            );
            assertThat(sequenceMapper.insert(eventId, LocalDateTime.now())).isEqualTo(1);
            ELASTICSEARCH.seedDocument(
                    stagingIndex,
                    "notice-" + aggregateId,
                    "{\"noticeId\":" + aggregateId + ",\"title\":\"公告\",\"content\":\"正文\","
                            + "\"publishedAt\":\"2026-07-22T09:00:00\",\"status\":\"PUBLISHED\"}"
            );
        }

        List<SearchIndexReplayDelta> firstPage = sequenceMapper.findProcessedAfter(
                "NOTICE", watermark, 100
        );
        List<SearchIndexReplayDelta> secondPage = sequenceMapper.findProcessedAfter(
                "NOTICE", firstPage.getLast().sequenceId(), 100
        );
        assertThat(firstPage).hasSize(100);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.getFirst().aggregateId()).isEqualTo(101L);

        cutoverFinalizer.finalizeNotices(stagingIndex, watermark);

        assertThat(ELASTICSEARCH.aliasTarget("oa-notices")).isEqualTo(stagingIndex);
        assertThat(ELASTICSEARCH.documents().keySet())
                .noneMatch(path -> path.startsWith("/" + stagingIndex + "/_doc/notice-"));
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

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted during test coordination", exception);
        }
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        StubSearchDocumentSourceGateway stubSearchDocumentSourceGateway() {
            return new StubSearchDocumentSourceGateway();
        }
    }

    static final class StubSearchDocumentSourceGateway implements SearchDocumentSourceGateway {

        private final Map<Long, NoticeSearchDocument> notices = new ConcurrentHashMap<>();
        private final Map<Long, ApplicationSearchDocument> applications = new ConcurrentHashMap<>();

        @Override
        public NoticeSearchDocument loadNotice(long noticeId) {
            return notices.get(noticeId);
        }

        @Override
        public ApplicationSearchDocument loadApplication(long applicationId) {
            return applications.get(applicationId);
        }

        private void clear() {
            notices.clear();
            applications.clear();
        }
    }
}
