package com.tsy.oa.intelligence.search.service;

import com.tsy.oa.intelligence.search.event.SearchIndexEvent;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexCutoverBarrierMapper;
import com.tsy.oa.intelligence.search.event.persistence.SearchIndexEventSequenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SearchIndexCutoverCoordinator {

    private final SearchIndexCutoverBarrierMapper barrierMapper;
    private final SearchIndexEventSequenceMapper sequenceMapper;

    public SearchIndexCutoverCoordinator(
            SearchIndexCutoverBarrierMapper barrierMapper,
            SearchIndexEventSequenceMapper sequenceMapper
    ) {
        this.barrierMapper = barrierMapper;
        this.sequenceMapper = sequenceMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(SearchIndexEvent.AggregateType aggregateType) {
        String type = aggregateType.name();
        int initialized = barrierMapper.initializeIfAbsent(type, now());
        if (initialized < 0 || initialized > 1) {
            throw new IllegalStateException("Failed to initialize search index cutover barrier");
        }
        if (!type.equals(barrierMapper.findForUpdate(type))) {
            throw new IllegalStateException("Search index cutover barrier is unavailable");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long captureWatermark(SearchIndexEvent.AggregateType aggregateType) {
        lock(aggregateType);
        return sequenceMapper.findMaximumSequence();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
