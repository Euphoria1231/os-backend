package com.tsy.oa.intelligence.search.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexAggregateState;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexAggregateStateMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexEventRecordMapper;
import com.tsy.oa.intelligence.search.event.source.SearchDocumentSourceGateway;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.repository.SearchIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class SearchIndexEventProcessor implements SearchIndexEventHandler {

    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_IGNORED_OUTDATED = "IGNORED_OUTDATED";

    private final SearchIndexEventRecordMapper eventRecordMapper;
    private final SearchIndexAggregateStateMapper aggregateStateMapper;
    private final SearchIndexRepository indexRepository;
    private final SearchDocumentSourceGateway sourceGateway;
    private final ObjectMapper objectMapper;
    private final SearchDocumentNormalizer documentNormalizer;

    public SearchIndexEventProcessor(
            SearchIndexEventRecordMapper eventRecordMapper,
            SearchIndexAggregateStateMapper aggregateStateMapper,
            SearchIndexRepository indexRepository,
            SearchDocumentSourceGateway sourceGateway,
            ObjectMapper objectMapper,
            SearchDocumentNormalizer documentNormalizer
    ) {
        this.eventRecordMapper = eventRecordMapper;
        this.aggregateStateMapper = aggregateStateMapper;
        this.indexRepository = indexRepository;
        this.sourceGateway = sourceGateway;
        this.objectMapper = objectMapper;
        this.documentNormalizer = documentNormalizer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SearchEventProcessingResult process(SearchIndexEvent event) throws IOException {
        Objects.requireNonNull(event, "event must not be null");
        String aggregateType = event.aggregateType().name();
        String operation = event.operation().name();

        int claimed = eventRecordMapper.claim(
                event.eventId(),
                aggregateType,
                event.aggregateId(),
                event.version(),
                operation
        );
        if (claimed == 0) {
            return SearchEventProcessingResult.DUPLICATE;
        }

        int initialized = aggregateStateMapper.initializeIfAbsent(
                aggregateType,
                event.aggregateId(),
                now()
        );
        if (initialized < 0 || initialized > 1) {
            throw new IllegalStateException("Failed to initialize search index aggregate state");
        }

        SearchIndexAggregateState state = aggregateStateMapper.findForUpdate(
                aggregateType,
                event.aggregateId()
        );
        if (state == null) {
            throw new IllegalStateException("Search index aggregate state is unavailable after initialization");
        }
        if (event.version() <= state.getLastEventVersion()) {
            markCompleted(event.eventId(), STATUS_IGNORED_OUTDATED);
            return SearchEventProcessingResult.IGNORED_OUTDATED;
        }

        applyEvent(event);
        saveAggregateState(event);
        markCompleted(event.eventId(), STATUS_PROCESSED);
        return SearchEventProcessingResult.PROCESSED;
    }

    private void applyEvent(SearchIndexEvent event) throws IOException {
        switch (event.aggregateType()) {
            case NOTICE -> applyNoticeEvent(event);
            case APPLICATION -> applyApplicationEvent(event);
        }
    }

    private void applyNoticeEvent(SearchIndexEvent event) throws IOException {
        if (event.operation() == SearchIndexEvent.Operation.DELETE) {
            indexRepository.deleteNotice(event.aggregateId());
            return;
        }
        NoticeSearchDocument document = readDocument(event, NoticeSearchDocument.class);
        if (document == null) {
            document = sourceGateway.loadNotice(event.aggregateId());
        }
        document = documentNormalizer.normalizeNotice(document);
        requireMatchingId(document.noticeId(), event.aggregateId(), "notice");
        indexRepository.saveNotice(document);
    }

    private void applyApplicationEvent(SearchIndexEvent event) throws IOException {
        if (event.operation() == SearchIndexEvent.Operation.DELETE) {
            indexRepository.deleteApplication(event.aggregateId());
            return;
        }
        ApplicationSearchDocument document = readDocument(event, ApplicationSearchDocument.class);
        if (document == null) {
            document = sourceGateway.loadApplication(event.aggregateId());
        }
        document = documentNormalizer.normalizeApplication(document);
        requireMatchingId(
                document.applicationId(),
                event.aggregateId(),
                "application"
        );
        indexRepository.saveApplication(document);
    }

    private <T> T readDocument(SearchIndexEvent event, Class<T> documentType) throws IOException {
        JsonNode document = event.document();
        if (document == null || document.isNull() || document.isMissingNode()) {
            return null;
        }
        return objectMapper.treeToValue(document, documentType);
    }

    private void requireMatchingId(long actualId, long expectedId, String aggregateType) {
        if (actualId <= 0) {
            throw new IllegalStateException(aggregateType + " search document is unavailable");
        }
        if (actualId != expectedId) {
            throw new IllegalStateException(aggregateType + " search document id does not match the event");
        }
    }

    private void saveAggregateState(SearchIndexEvent event) {
        int affectedRows = aggregateStateMapper.update(
                event.aggregateType().name(),
                event.aggregateId(),
                event.version(),
                event.eventId(),
                event.operation().name(),
                now()
        );
        if (affectedRows != 1) {
            throw new IllegalStateException("Failed to persist search index aggregate state");
        }
    }

    private void markCompleted(String eventId, String processingStatus) {
        int affectedRows = eventRecordMapper.markCompleted(eventId, processingStatus, now());
        if (affectedRows != 1) {
            throw new IllegalStateException("Failed to persist search index event result");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
