CREATE TABLE IF NOT EXISTS search_index_event_record (
    event_id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_version BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    processing_status VARCHAR(32) NOT NULL,
    processed_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS search_index_aggregate_state (
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    last_event_version BIGINT NOT NULL,
    last_event_id VARCHAR(64) NOT NULL,
    last_operation VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (aggregate_type, aggregate_id)
);
