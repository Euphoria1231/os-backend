package com.tsy.oa.flow.search;

import com.fasterxml.jackson.databind.JsonNode;

public record SearchIndexEvent(
        String eventId,
        AggregateType aggregateType,
        Operation operation,
        long aggregateId,
        long version,
        JsonNode document
) {

    private static final int MAX_EVENT_ID_LENGTH = 64;

    public SearchIndexEvent {
        if (eventId == null || eventId.isBlank() || eventId.length() > MAX_EVENT_ID_LENGTH) {
            throw new IllegalArgumentException("eventId must contain between 1 and 64 characters");
        }
        if (aggregateType == null) {
            throw new IllegalArgumentException("aggregateType must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (aggregateId <= 0) {
            throw new IllegalArgumentException("aggregateId must be positive");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }
    }

    public enum AggregateType {
        APPLICATION
    }

    public enum Operation {
        UPSERT
    }
}
