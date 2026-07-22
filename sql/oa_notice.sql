CREATE DATABASE IF NOT EXISTS oa_notice
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_notice;

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告正文',
    publisher_id BIGINT NOT NULL COMMENT '发布人员工ID',
    status VARCHAR(20) NOT NULL COMMENT '状态：PUBLISHED',
    published_at DATETIME NOT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_notice_status_published (status, published_at),
    KEY idx_notice_updated_id (updated_at, id)
) ENGINE=InnoDB COMMENT='公司公告表';

CREATE TABLE IF NOT EXISTS message_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Message consume record ID',
    event_id VARCHAR(100) NOT NULL COMMENT 'Event ID',
    topic VARCHAR(100) NOT NULL COMMENT 'RocketMQ Topic',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Consumed time',
    UNIQUE KEY uk_message_consume_event (event_id)
) ENGINE=InnoDB COMMENT='Message consume idempotency table';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Message ID',
    recipient_id BIGINT NOT NULL COMMENT 'Recipient employee ID',
    title VARCHAR(200) NOT NULL COMMENT 'Message title',
    content VARCHAR(1000) NOT NULL COMMENT 'Message content',
    business_type VARCHAR(50) NOT NULL COMMENT 'Business type',
    business_id BIGINT NOT NULL COMMENT 'Business ID',
    source_event_id VARCHAR(100) NOT NULL COMMENT 'Source event ID',
    read_at DATETIME NULL COMMENT 'Read time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    UNIQUE KEY uk_message_event_recipient (source_event_id, recipient_id),
    KEY idx_message_recipient_created (recipient_id, created_at),
    KEY idx_message_recipient_read (recipient_id, read_at)
) ENGINE=InnoDB COMMENT='Message table';

CREATE TABLE IF NOT EXISTS notice_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阅读记录ID',
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    employee_id BIGINT NOT NULL COMMENT '阅读人员工ID',
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_notice_read_employee (notice_id, employee_id),
    KEY idx_notice_read_employee (employee_id, read_at)
) ENGINE=InnoDB COMMENT='公告阅读状态表';
