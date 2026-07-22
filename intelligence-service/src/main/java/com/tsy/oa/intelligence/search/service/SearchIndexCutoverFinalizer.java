package com.tsy.oa.intelligence.search.service;

import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.event.SearchDocumentNormalizer;
import com.tsy.oa.intelligence.search.event.SearchIndexEvent;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexEventSequenceMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexReplayDelta;
import com.tsy.oa.intelligence.search.event.source.SearchDocumentSourceGateway;
import com.tsy.oa.intelligence.search.repository.ElasticsearchGateway;
import com.tsy.oa.intelligence.search.repository.SearchIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class SearchIndexCutoverFinalizer {

    private static final int REPLAY_PAGE_SIZE = 100;

    private final SearchIndexCutoverCoordinator coordinator;
    private final SearchIndexEventSequenceMapper sequenceMapper;
    private final SearchDocumentSourceGateway sourceGateway;
    private final SearchDocumentNormalizer documentNormalizer;
    private final SearchIndexRepository indexRepository;
    private final ElasticsearchGateway gateway;
    private final ElasticsearchSearchProperties properties;

    public SearchIndexCutoverFinalizer(
            SearchIndexCutoverCoordinator coordinator,
            SearchIndexEventSequenceMapper sequenceMapper,
            SearchDocumentSourceGateway sourceGateway,
            SearchDocumentNormalizer documentNormalizer,
            SearchIndexRepository indexRepository,
            ElasticsearchGateway gateway,
            ElasticsearchSearchProperties properties
    ) {
        this.coordinator = coordinator;
        this.sequenceMapper = sequenceMapper;
        this.sourceGateway = sourceGateway;
        this.documentNormalizer = documentNormalizer;
        this.indexRepository = indexRepository;
        this.gateway = gateway;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeNotices(String stagingIndex, long watermark) throws IOException {
        coordinator.lock(SearchIndexEvent.AggregateType.NOTICE);
        replayNotices(stagingIndex, watermark);
        gateway.refreshIndex(stagingIndex);
        gateway.switchAlias(properties.getNoticeAlias(), stagingIndex);
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeApplications(String stagingIndex, long watermark) throws IOException {
        coordinator.lock(SearchIndexEvent.AggregateType.APPLICATION);
        replayApplications(stagingIndex, watermark);
        gateway.refreshIndex(stagingIndex);
        gateway.switchAlias(properties.getApplicationAlias(), stagingIndex);
    }

    private void replayNotices(String stagingIndex, long watermark) throws IOException {
        long cursor = watermark;
        while (true) {
            List<SearchIndexReplayDelta> deltas = sequenceMapper.findProcessedAfter(
                    SearchIndexEvent.AggregateType.NOTICE.name(), cursor, REPLAY_PAGE_SIZE
            );
            for (SearchIndexReplayDelta delta : deltas) {
                applyNoticeDelta(stagingIndex, delta);
                cursor = delta.sequenceId();
            }
            if (deltas.size() < REPLAY_PAGE_SIZE) {
                return;
            }
        }
    }

    private void replayApplications(String stagingIndex, long watermark) throws IOException {
        long cursor = watermark;
        while (true) {
            List<SearchIndexReplayDelta> deltas = sequenceMapper.findProcessedAfter(
                    SearchIndexEvent.AggregateType.APPLICATION.name(), cursor, REPLAY_PAGE_SIZE
            );
            for (SearchIndexReplayDelta delta : deltas) {
                applyApplicationDelta(stagingIndex, delta);
                cursor = delta.sequenceId();
            }
            if (deltas.size() < REPLAY_PAGE_SIZE) {
                return;
            }
        }
    }

    private void applyNoticeDelta(String stagingIndex, SearchIndexReplayDelta delta) throws IOException {
        if (SearchIndexEvent.Operation.DELETE.name().equals(delta.operation())) {
            indexRepository.deleteNoticeFromIndex(stagingIndex, delta.aggregateId());
            return;
        }
        var source = sourceGateway.findNotice(delta.aggregateId());
        if (source.isEmpty()) {
            indexRepository.deleteNoticeFromIndex(stagingIndex, delta.aggregateId());
            return;
        }
        indexRepository.saveNoticeToIndex(
                stagingIndex,
                documentNormalizer.normalizeNotice(source.get())
        );
    }

    private void applyApplicationDelta(String stagingIndex, SearchIndexReplayDelta delta) throws IOException {
        if (SearchIndexEvent.Operation.DELETE.name().equals(delta.operation())) {
            indexRepository.deleteApplicationFromIndex(stagingIndex, delta.aggregateId());
            return;
        }
        var source = sourceGateway.findApplication(delta.aggregateId());
        if (source.isEmpty()) {
            indexRepository.deleteApplicationFromIndex(stagingIndex, delta.aggregateId());
            return;
        }
        indexRepository.saveApplicationToIndex(
                stagingIndex,
                documentNormalizer.normalizeApplication(source.get())
        );
    }
}
