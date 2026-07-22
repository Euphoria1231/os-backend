package com.tsy.oa.intelligence.search.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.IntelligenceServiceApplication;
import com.tsy.oa.intelligence.search.event.source.SearchDocumentSourceGateway;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.repository.SearchIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = IntelligenceServiceApplication.class)
@Import(SearchIndexEventProcessorTests.TestBeans.class)
class SearchIndexEventProcessorTests {

    @Autowired
    private SearchIndexEventProcessor processor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InMemorySearchIndexRepository repository;

    @Autowired
    private StubSearchDocumentSourceGateway sourceGateway;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM search_index_event_record");
        jdbcTemplate.update("DELETE FROM search_index_aggregate_state");
        repository.clear();
        sourceGateway.clear();
    }

    @Test
    void duplicateEventIdWritesTheIndexOnlyOnce() throws Exception {
        SearchIndexEvent event = noticeEvent("notice-event-1", 42L, 1L, noticeDocument(42L, "第一版"));

        assertThat(processor.process(event)).isEqualTo(SearchEventProcessingResult.PROCESSED);
        assertThat(processor.process(event)).isEqualTo(SearchEventProcessingResult.DUPLICATE);

        assertThat(repository.noticeWriteCount).isEqualTo(1);
        assertThat(eventStatus("notice-event-1")).isEqualTo("PROCESSED");
    }

    @Test
    void staleUpdateDoesNotOverwriteANewerDocument() throws Exception {
        processor.process(noticeEvent("notice-event-2", 42L, 2L, noticeDocument(42L, "第二版")));

        SearchEventProcessingResult result = processor.process(
                noticeEvent("notice-event-1", 42L, 1L, noticeDocument(42L, "过期第一版"))
        );

        assertThat(result).isEqualTo(SearchEventProcessingResult.IGNORED_OUTDATED);
        assertThat(repository.notices.get(42L).title()).isEqualTo("第二版");
        assertThat(repository.noticeWriteCount).isEqualTo(1);
        assertThat(eventStatus("notice-event-1")).isEqualTo("IGNORED_OUTDATED");
        assertThat(lastVersion("NOTICE", 42L)).isEqualTo(2L);
    }

    @Test
    void staleDeleteDoesNotRemoveANewerDocument() throws Exception {
        processor.process(noticeEvent("notice-event-2", 42L, 2L, noticeDocument(42L, "有效版本")));

        SearchEventProcessingResult result = processor.process(deleteEvent("notice-delete-1", 42L, 1L));

        assertThat(result).isEqualTo(SearchEventProcessingResult.IGNORED_OUTDATED);
        assertThat(repository.notices).containsKey(42L);
        assertThat(repository.noticeDeleteCount).isZero();
    }

    @Test
    void newerDeleteRemovesTheIndexedDocument() throws Exception {
        processor.process(noticeEvent("notice-event-1", 42L, 1L, noticeDocument(42L, "待删除")));

        SearchEventProcessingResult result = processor.process(deleteEvent("notice-delete-2", 42L, 2L));

        assertThat(result).isEqualTo(SearchEventProcessingResult.PROCESSED);
        assertThat(repository.notices).doesNotContainKey(42L);
        assertThat(repository.noticeDeleteCount).isEqualTo(1);
        assertThat(lastVersion("NOTICE", 42L)).isEqualTo(2L);
        assertThat(lastOperation("NOTICE", 42L)).isEqualTo("DELETE");
    }

    @Test
    void missingApplicationDocumentIsLoadedFromTheSourceService() throws Exception {
        ApplicationSearchDocument sourceDocument = new ApplicationSearchDocument(
                15L,
                3L,
                "LEAVE",
                "APPROVED",
                "身体不适",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 12, 0)
        );
        sourceGateway.applications.put(15L, sourceDocument);
        SearchIndexEvent event = new SearchIndexEvent(
                "application-event-1",
                SearchIndexEvent.AggregateType.APPLICATION,
                SearchIndexEvent.Operation.UPSERT,
                15L,
                1L,
                null
        );

        assertThat(processor.process(event)).isEqualTo(SearchEventProcessingResult.PROCESSED);

        assertThat(sourceGateway.applicationLoadCount).isEqualTo(1);
        assertThat(repository.applications.get(15L)).isEqualTo(sourceDocument);
    }

    @Test
    void inlineApplicationDocumentUsesTheSameSummaryLimitAsSourceData() throws Exception {
        ApplicationSearchDocument document = new ApplicationSearchDocument(
                15L,
                3L,
                "LEAVE",
                "PENDING",
                "病".repeat(600),
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 8, 30)
        );
        SearchIndexEvent event = new SearchIndexEvent(
                "application-inline-summary",
                SearchIndexEvent.AggregateType.APPLICATION,
                SearchIndexEvent.Operation.UPSERT,
                15L,
                1L,
                objectMapper.valueToTree(document)
        );

        assertThat(processor.process(event)).isEqualTo(SearchEventProcessingResult.PROCESSED);

        assertThat(repository.applications.get(15L).reasonSummary()).hasSize(500);
    }

    @Test
    void invalidInlineApplicationDocumentIsRejectedBeforeIndexWrite() {
        ApplicationSearchDocument document = new ApplicationSearchDocument(
                15L,
                0L,
                "LEAVE",
                "PENDING",
                "申请原因",
                LocalDateTime.of(2026, 7, 22, 8, 30),
                LocalDateTime.of(2026, 7, 22, 8, 30)
        );
        SearchIndexEvent event = new SearchIndexEvent(
                "application-inline-invalid",
                SearchIndexEvent.AggregateType.APPLICATION,
                SearchIndexEvent.Operation.UPSERT,
                15L,
                1L,
                objectMapper.valueToTree(document)
        );

        assertThatThrownBy(() -> processor.process(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("applicant");
        assertThat(repository.applications).doesNotContainKey(15L);
        assertThat(eventCount("application-inline-invalid")).isZero();
    }

    @Test
    void failedIndexWriteRollsBackTheEventClaimForRetry() throws Exception {
        SearchIndexEvent event = noticeEvent("notice-retry-1", 42L, 1L, noticeDocument(42L, "重试事件"));
        repository.failNextWrite = true;

        assertThatThrownBy(() -> processor.process(event))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("simulated index failure");
        assertThat(eventCount("notice-retry-1")).isZero();

        assertThat(processor.process(event)).isEqualTo(SearchEventProcessingResult.PROCESSED);
        assertThat(eventCount("notice-retry-1")).isEqualTo(1);
    }

    @Test
    void concurrentFirstEventsAreSerializedByAggregateVersion() throws Exception {
        repository.coordinateConcurrentInitialWrites();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<SearchEventProcessingResult> firstVersion = executor.submit(
                    () -> processor.process(
                            noticeEvent("notice-concurrent-1", 42L, 1L, noticeDocument(42L, "第一版"))
                    )
            );
            assertThat(repository.awaitFirstVersionWrite()).isTrue();

            Future<SearchEventProcessingResult> secondVersion = executor.submit(
                    () -> processor.process(
                            noticeEvent("notice-concurrent-2", 42L, 2L, noticeDocument(42L, "第二版"))
                    )
            );
            repository.awaitPotentialSecondVersionWrite();
            repository.releaseFirstVersionWrite();

            assertThat(firstVersion.get(5, TimeUnit.SECONDS))
                    .isEqualTo(SearchEventProcessingResult.PROCESSED);
            assertThat(secondVersion.get(5, TimeUnit.SECONDS))
                    .isEqualTo(SearchEventProcessingResult.PROCESSED);
        } finally {
            repository.releaseFirstVersionWrite();
            executor.shutdownNow();
        }

        assertThat(lastVersion("NOTICE", 42L)).isEqualTo(2L);
        assertThat(repository.notices.get(42L).title()).isEqualTo("第二版");
    }

    private SearchIndexEvent noticeEvent(
            String eventId,
            long noticeId,
            long version,
            NoticeSearchDocument document
    ) {
        JsonNode documentNode = objectMapper.valueToTree(document);
        return new SearchIndexEvent(
                eventId,
                SearchIndexEvent.AggregateType.NOTICE,
                SearchIndexEvent.Operation.UPSERT,
                noticeId,
                version,
                documentNode
        );
    }

    private SearchIndexEvent deleteEvent(String eventId, long noticeId, long version) {
        return new SearchIndexEvent(
                eventId,
                SearchIndexEvent.AggregateType.NOTICE,
                SearchIndexEvent.Operation.DELETE,
                noticeId,
                version,
                null
        );
    }

    private NoticeSearchDocument noticeDocument(long noticeId, String title) {
        return new NoticeSearchDocument(
                noticeId,
                title,
                title + "正文",
                LocalDateTime.of(2026, 7, 22, 9, 0),
                "PUBLISHED"
        );
    }

    private String eventStatus(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT processing_status FROM search_index_event_record WHERE event_id = ?",
                String.class,
                eventId
        );
    }

    private int eventCount(String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM search_index_event_record WHERE event_id = ?",
                Integer.class,
                eventId
        );
        return count == null ? 0 : count;
    }

    private long lastVersion(String aggregateType, long aggregateId) {
        Long version = jdbcTemplate.queryForObject(
                "SELECT last_event_version FROM search_index_aggregate_state "
                        + "WHERE aggregate_type = ? AND aggregate_id = ?",
                Long.class,
                aggregateType,
                aggregateId
        );
        return version == null ? 0L : version;
    }

    private String lastOperation(String aggregateType, long aggregateId) {
        return jdbcTemplate.queryForObject(
                "SELECT last_operation FROM search_index_aggregate_state "
                        + "WHERE aggregate_type = ? AND aggregate_id = ?",
                String.class,
                aggregateType,
                aggregateId
        );
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        InMemorySearchIndexRepository inMemorySearchIndexRepository() {
            return new InMemorySearchIndexRepository();
        }

        @Bean
        @Primary
        StubSearchDocumentSourceGateway stubSearchDocumentSourceGateway() {
            return new StubSearchDocumentSourceGateway();
        }
    }

    static final class InMemorySearchIndexRepository implements SearchIndexRepository {

        private final Map<Long, NoticeSearchDocument> notices = new ConcurrentHashMap<>();
        private final Map<Long, ApplicationSearchDocument> applications = new ConcurrentHashMap<>();
        private int noticeWriteCount;
        private int noticeDeleteCount;
        private boolean failNextWrite;
        private boolean coordinateConcurrentInitialWrites;
        private CountDownLatch firstVersionWriteEntered = new CountDownLatch(0);
        private CountDownLatch secondVersionWriteCompleted = new CountDownLatch(0);
        private CountDownLatch releaseFirstVersionWrite = new CountDownLatch(0);

        @Override
        public void saveNotice(NoticeSearchDocument document) throws IOException {
            failIfRequested();
            coordinateWriteIfRequested(document);
            noticeWriteCount++;
            notices.put(document.noticeId(), document);
        }

        @Override
        public void deleteNotice(long noticeId) throws IOException {
            failIfRequested();
            noticeDeleteCount++;
            notices.remove(noticeId);
        }

        @Override
        public void saveApplication(ApplicationSearchDocument document) throws IOException {
            failIfRequested();
            applications.put(document.applicationId(), document);
        }

        @Override
        public void deleteApplication(long applicationId) throws IOException {
            failIfRequested();
            applications.remove(applicationId);
        }

        private void failIfRequested() throws IOException {
            if (failNextWrite) {
                failNextWrite = false;
                throw new IOException("simulated index failure");
            }
        }

        private void coordinateWriteIfRequested(NoticeSearchDocument document) throws IOException {
            if (!coordinateConcurrentInitialWrites) {
                return;
            }
            if ("第一版".equals(document.title())) {
                firstVersionWriteEntered.countDown();
                awaitReleaseFirstVersionWrite();
                return;
            }
            if ("第二版".equals(document.title())) {
                notices.put(document.noticeId(), document);
                secondVersionWriteCompleted.countDown();
            }
        }

        private void awaitReleaseFirstVersionWrite() throws IOException {
            try {
                if (!releaseFirstVersionWrite.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("timed out waiting to release first version write");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while coordinating concurrent writes", exception);
            }
        }

        private void coordinateConcurrentInitialWrites() {
            coordinateConcurrentInitialWrites = true;
            firstVersionWriteEntered = new CountDownLatch(1);
            secondVersionWriteCompleted = new CountDownLatch(1);
            releaseFirstVersionWrite = new CountDownLatch(1);
        }

        private boolean awaitFirstVersionWrite() throws InterruptedException {
            return firstVersionWriteEntered.await(5, TimeUnit.SECONDS);
        }

        private void awaitPotentialSecondVersionWrite() throws InterruptedException {
            secondVersionWriteCompleted.await(2, TimeUnit.SECONDS);
        }

        private void releaseFirstVersionWrite() {
            releaseFirstVersionWrite.countDown();
        }

        private void clear() {
            notices.clear();
            applications.clear();
            noticeWriteCount = 0;
            noticeDeleteCount = 0;
            failNextWrite = false;
            coordinateConcurrentInitialWrites = false;
            firstVersionWriteEntered = new CountDownLatch(0);
            secondVersionWriteCompleted = new CountDownLatch(0);
            releaseFirstVersionWrite = new CountDownLatch(0);
        }
    }

    static final class StubSearchDocumentSourceGateway implements SearchDocumentSourceGateway {

        private final Map<Long, NoticeSearchDocument> notices = new HashMap<>();
        private final Map<Long, ApplicationSearchDocument> applications = new HashMap<>();
        private int applicationLoadCount;

        @Override
        public NoticeSearchDocument loadNotice(long noticeId) {
            return notices.get(noticeId);
        }

        @Override
        public ApplicationSearchDocument loadApplication(long applicationId) {
            applicationLoadCount++;
            return applications.get(applicationId);
        }

        private void clear() {
            notices.clear();
            applications.clear();
            applicationLoadCount = 0;
        }
    }
}
