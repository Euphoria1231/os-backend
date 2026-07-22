CREATE DATABASE IF NOT EXISTS oa_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_ai;

CREATE TABLE IF NOT EXISTS search_index_event_record (
    event_id VARCHAR(64) PRIMARY KEY COMMENT '全局唯一事件ID',
    aggregate_type VARCHAR(32) NOT NULL COMMENT 'NOTICE或APPLICATION',
    aggregate_id BIGINT NOT NULL COMMENT '业务聚合ID',
    event_version BIGINT NOT NULL COMMENT '聚合内单调递增版本',
    operation VARCHAR(16) NOT NULL COMMENT 'UPSERT或DELETE',
    processing_status VARCHAR(32) NOT NULL COMMENT 'PROCESSING、PROCESSED或IGNORED_OUTDATED',
    processed_at DATETIME(3) NULL COMMENT '处理完成时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件首次接收时间',
    KEY idx_search_event_aggregate (aggregate_type, aggregate_id, event_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='搜索索引事件幂等记录';

CREATE TABLE IF NOT EXISTS search_index_aggregate_state (
    aggregate_type VARCHAR(32) NOT NULL COMMENT 'NOTICE或APPLICATION',
    aggregate_id BIGINT NOT NULL COMMENT '业务聚合ID',
    last_event_version BIGINT NOT NULL COMMENT '最后成功处理的事件版本',
    last_event_id VARCHAR(64) NOT NULL COMMENT '最后成功处理的事件ID',
    last_operation VARCHAR(16) NOT NULL COMMENT 'INIT、UPSERT或DELETE',
    updated_at DATETIME(3) NOT NULL COMMENT '状态更新时间',
    PRIMARY KEY (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='搜索索引聚合版本状态';
