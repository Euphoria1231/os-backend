package com.tsy.oa.intelligence.search.service;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.dto.IndexHealthResponse;
import com.tsy.oa.intelligence.search.dto.RebuildProgressResponse;
import com.tsy.oa.intelligence.search.event.SearchDocumentNormalizer;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourcePageClient;
import com.tsy.oa.intelligence.search.event.source.NoticeSearchSourceClient;
import com.tsy.oa.intelligence.search.event.source.NoticeSearchSourcePageClient;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.repository.ElasticsearchGateway;
import com.tsy.oa.intelligence.search.repository.SearchIndexRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SearchIndexAdministrationService {

    private static final int REBUILD_PAGE_SIZE = 100;
    private static final String DELETE_ALL_DOCUMENTS = "{\"query\":{\"match_all\":{}}}";

    private final ElasticsearchGateway gateway;
    private final ElasticsearchSearchProperties properties;
    private final NoticeSearchSourcePageClient noticeSourceClient;
    private final ApplicationSearchSourcePageClient applicationSourceClient;
    private final SearchIndexRepository searchIndexRepository;
    private final SearchDocumentNormalizer documentNormalizer;
    private final AtomicBoolean noticeRebuildRunning = new AtomicBoolean();
    private final AtomicBoolean applicationRebuildRunning = new AtomicBoolean();
    private final AtomicReference<RebuildProgressResponse> noticeProgress =
            new AtomicReference<>(RebuildProgressResponse.idle("NOTICES"));
    private final AtomicReference<RebuildProgressResponse> applicationProgress =
            new AtomicReference<>(RebuildProgressResponse.idle("APPLICATIONS"));

    public SearchIndexAdministrationService(
            ElasticsearchGateway gateway,
            ElasticsearchSearchProperties properties,
            NoticeSearchSourcePageClient noticeSourceClient,
            ApplicationSearchSourcePageClient applicationSourceClient,
            SearchIndexRepository searchIndexRepository,
            SearchDocumentNormalizer documentNormalizer
    ) {
        this.gateway = gateway;
        this.properties = properties;
        this.noticeSourceClient = noticeSourceClient;
        this.applicationSourceClient = applicationSourceClient;
        this.searchIndexRepository = searchIndexRepository;
        this.documentNormalizer = documentNormalizer;
    }

    public RebuildProgressResponse rebuildNotices() {
        requireNotRunning(noticeRebuildRunning);
        LocalDateTime startedAt = LocalDateTime.now();
        noticeProgress.set(running("NOTICES", startedAt));
        try {
            gateway.deleteByQuery(properties.getNoticeIndex(), DELETE_ALL_DOCUMENTS);
            int page = 1;
            long processed = 0;
            Long expectedTotal = null;
            while (true) {
                NoticeSearchSourcePageClient.NoticeSearchSourcePageResponse sourcePage = requirePage(
                        noticeSourceClient.getPage(page, REBUILD_PAGE_SIZE), "notice"
                );
                expectedTotal = validatePage(
                        "notice", page, sourcePage.page(), sourcePage.pageSize(), sourcePage.total(),
                        expectedTotal, sourcePage.items()
                );
                List<NoticeSearchDocument> documents = sourcePage.items().stream()
                        .map(this::toNoticeDocument)
                        .toList();
                searchIndexRepository.saveNotices(documents);
                processed += documents.size();
                requireProcessedNotBeyondTotal(processed, expectedTotal, "notice");
                noticeProgress.set(progress(
                        "NOTICES", "RUNNING", processed, sourcePage.total(), page, startedAt, null, null
                ));
                if (!sourcePage.hasNext()) {
                    requireComplete(processed, expectedTotal, "notice");
                    gateway.refreshIndex(properties.getNoticeIndex());
                    return complete(noticeProgress, "NOTICES", processed, expectedTotal, page, startedAt);
                }
                requireProgress(documents, "notice");
                page++;
            }
        } catch (Exception exception) {
            fail(noticeProgress, "NOTICES", startedAt, exception);
            throw new IllegalStateException("Notice index rebuild failed", exception);
        } finally {
            noticeRebuildRunning.set(false);
        }
    }

    public RebuildProgressResponse rebuildApplications() {
        requireNotRunning(applicationRebuildRunning);
        LocalDateTime startedAt = LocalDateTime.now();
        applicationProgress.set(running("APPLICATIONS", startedAt));
        try {
            gateway.deleteByQuery(properties.getApplicationIndex(), DELETE_ALL_DOCUMENTS);
            int page = 1;
            long processed = 0;
            Long expectedTotal = null;
            while (true) {
                ApplicationSearchSourcePageClient.ApplicationSearchSourcePageResponse sourcePage = requirePage(
                        applicationSourceClient.getPage(page, REBUILD_PAGE_SIZE), "application"
                );
                expectedTotal = validatePage(
                        "application", page, sourcePage.page(), sourcePage.pageSize(), sourcePage.total(),
                        expectedTotal, sourcePage.items()
                );
                List<ApplicationSearchDocument> documents = sourcePage.items().stream()
                        .map(this::toApplicationDocument)
                        .toList();
                searchIndexRepository.saveApplications(documents);
                processed += documents.size();
                requireProcessedNotBeyondTotal(processed, expectedTotal, "application");
                applicationProgress.set(progress(
                        "APPLICATIONS", "RUNNING", processed, sourcePage.total(), page, startedAt, null, null
                ));
                if (!sourcePage.hasNext()) {
                    requireComplete(processed, expectedTotal, "application");
                    gateway.refreshIndex(properties.getApplicationIndex());
                    return complete(
                            applicationProgress, "APPLICATIONS", processed, expectedTotal, page, startedAt
                    );
                }
                requireProgress(documents, "application");
                page++;
            }
        } catch (Exception exception) {
            fail(applicationProgress, "APPLICATIONS", startedAt, exception);
            throw new IllegalStateException("Application index rebuild failed", exception);
        } finally {
            applicationRebuildRunning.set(false);
        }
    }

    public IndexHealthResponse health() {
        try {
            boolean noticeExists = gateway.indexExists(properties.getNoticeIndex());
            boolean applicationExists = gateway.indexExists(properties.getApplicationIndex());
            return new IndexHealthResponse(
                    true,
                    new IndexHealthResponse.IndexState(properties.getNoticeIndex(), noticeExists),
                    new IndexHealthResponse.IndexState(properties.getApplicationIndex(), applicationExists),
                    noticeProgress.get(),
                    applicationProgress.get()
            );
        } catch (IOException exception) {
            return new IndexHealthResponse(
                    false,
                    new IndexHealthResponse.IndexState(properties.getNoticeIndex(), false),
                    new IndexHealthResponse.IndexState(properties.getApplicationIndex(), false),
                    noticeProgress.get(),
                    applicationProgress.get()
            );
        }
    }

    private NoticeSearchDocument toNoticeDocument(
            NoticeSearchSourceClient.NoticeSearchSourceResponse source
    ) {
        return documentNormalizer.normalizeNotice(new NoticeSearchDocument(
                requireId(source.id(), "notice"), source.title(), source.content(),
                source.publishedAt(), source.status()
        ));
    }

    private ApplicationSearchDocument toApplicationDocument(
            ApplicationSearchSourceClient.ApplicationSearchSourceResponse source
    ) {
        return documentNormalizer.normalizeApplication(new ApplicationSearchDocument(
                requireId(source.id(), "application"),
                requireId(source.applicantId(), "application applicant"),
                requireId(source.approverId(), "application approver"),
                source.applicationType(), source.status(), source.reason(),
                source.createdAt(), source.updatedAt()
        ));
    }

    private <T> T requirePage(ApiResponse<T> response, String sourceType) {
        if (response == null || response.code() != 0 || response.data() == null) {
            throw new IllegalStateException(sourceType + " search source page is unavailable");
        }
        return response.data();
    }

    private long requireId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(fieldName + " id is missing");
        }
        return value;
    }

    private Long validatePage(
            String sourceType,
            int expectedPage,
            int actualPage,
            int actualPageSize,
            long actualTotal,
            Long expectedTotal,
            List<?> items
    ) {
        if (actualPage != expectedPage || actualPageSize != REBUILD_PAGE_SIZE
                || actualTotal < 0 || items == null || items.size() > REBUILD_PAGE_SIZE) {
            throw new IllegalStateException(sourceType + " search source pagination is invalid");
        }
        if (expectedTotal != null && expectedTotal.longValue() != actualTotal) {
            throw new IllegalStateException(sourceType + " search source total changed during rebuild");
        }
        return actualTotal;
    }

    private void requireProcessedNotBeyondTotal(long processed, long total, String sourceType) {
        if (processed > total) {
            throw new IllegalStateException(sourceType + " search source exceeded declared total");
        }
    }

    private void requireComplete(long processed, long total, String sourceType) {
        if (processed != total) {
            throw new IllegalStateException(sourceType + " search source ended before declared total");
        }
    }

    private void requireProgress(List<?> documents, String sourceType) {
        if (documents.isEmpty()) {
            throw new IllegalStateException(sourceType + " search source returned an empty continuing page");
        }
    }

    private void requireNotRunning(AtomicBoolean running) {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private RebuildProgressResponse running(String type, LocalDateTime startedAt) {
        return progress(type, "RUNNING", 0, 0, 0, startedAt, null, null);
    }

    private RebuildProgressResponse complete(
            AtomicReference<RebuildProgressResponse> state,
            String type,
            long processed,
            long total,
            int page,
            LocalDateTime startedAt
    ) {
        RebuildProgressResponse completed = progress(
                type, "COMPLETED", processed, total, page, startedAt, LocalDateTime.now(), null
        );
        state.set(completed);
        return completed;
    }

    private void fail(
            AtomicReference<RebuildProgressResponse> state,
            String type,
            LocalDateTime startedAt,
            Exception exception
    ) {
        RebuildProgressResponse current = state.get();
        state.set(progress(
                type, "FAILED", current.processed(), current.total(), current.currentPage(),
                startedAt, LocalDateTime.now(), exception.getClass().getSimpleName()
        ));
    }

    private RebuildProgressResponse progress(
            String type,
            String status,
            long processed,
            long total,
            int page,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String message
    ) {
        return new RebuildProgressResponse(
                type, status, processed, total, page, startedAt, completedAt, message
        );
    }
}
