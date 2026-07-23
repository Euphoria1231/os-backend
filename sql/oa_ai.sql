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

CREATE TABLE IF NOT EXISTS search_index_cutover_barrier (
    aggregate_type VARCHAR(32) PRIMARY KEY COMMENT 'NOTICE或APPLICATION',
    updated_at DATETIME(3) NOT NULL COMMENT '屏障初始化时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='搜索索引重建切换屏障';

CREATE TABLE IF NOT EXISTS search_index_event_sequence (
    sequence_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '全局严格递增事件序号',
    event_id VARCHAR(64) NOT NULL COMMENT '搜索索引事件ID',
    created_at DATETIME(3) NOT NULL COMMENT '序号分配时间',
    PRIMARY KEY (sequence_id),
    UNIQUE KEY uk_search_event_sequence_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='搜索索引事件全局顺序';

CREATE TABLE IF NOT EXISTS ai_analysis_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计记录ID',
    request_type VARCHAR(64) NOT NULL COMMENT '请求类型',
    business_reference_id VARCHAR(128) NOT NULL COMMENT '业务关联ID',
    initiator_employee_id BIGINT NULL COMMENT '发起智能分析的员工ID；历史无归属记录仅SUPER_ADMIN可查',
    status VARCHAR(16) NOT NULL COMMENT 'SUCCESS、DEGRADED或FAILED',
    duration_ms BIGINT NOT NULL COMMENT 'AI调用耗时毫秒',
    result_summary VARCHAR(500) NOT NULL COMMENT '必要结果摘要，不保存完整请求',
    audited_at DATETIME(3) NOT NULL COMMENT '审计时间',
    PRIMARY KEY (id),
    KEY idx_ai_analysis_business (request_type, business_reference_id),
    KEY idx_ai_analysis_initiator_audited_at (initiator_employee_id, audited_at),
    KEY idx_ai_analysis_audited_at (audited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='AI辅助分析调用审计记录';
