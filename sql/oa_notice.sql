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
    search_version BIGINT NOT NULL DEFAULT 1 COMMENT '搜索索引事件版本',
    KEY idx_notice_status_published (status, published_at)
) ENGINE=InnoDB COMMENT='公司公告表';

CREATE TABLE IF NOT EXISTS notice_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阅读记录ID',
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    employee_id BIGINT NOT NULL COMMENT '阅读人员工ID',
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_notice_read_employee (notice_id, employee_id),
    KEY idx_notice_read_employee (employee_id, read_at)
) ENGINE=InnoDB COMMENT='公告阅读状态表';

CREATE TABLE IF NOT EXISTS personal_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '个人通知ID',
    recipient_employee_id BIGINT NOT NULL COMMENT '接收人员工ID',
    notification_type VARCHAR(50) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    related_business_type VARCHAR(50) NOT NULL COMMENT '关联业务类型',
    related_business_id BIGINT NOT NULL COMMENT '关联业务ID',
    event_id VARCHAR(64) NOT NULL COMMENT '来源事件幂等ID',
    read_at DATETIME NULL COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_personal_notification_event (event_id),
    KEY idx_personal_notification_recipient (recipient_employee_id, read_at, created_at)
) ENGINE=InnoDB COMMENT='个人站内通知表';
