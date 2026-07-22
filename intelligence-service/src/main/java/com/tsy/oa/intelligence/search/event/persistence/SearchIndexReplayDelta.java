package com.tsy.oa.intelligence.search.event.persistence;

public record SearchIndexReplayDelta(
        long sequenceId,
        long aggregateId,
        String operation
) {
}
