CREATE DATABASE IF NOT EXISTS oa_flow
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_flow;

CREATE TABLE IF NOT EXISTS flow_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    application_no VARCHAR(50) NOT NULL COMMENT '申请单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人员工ID',
    approver_id BIGINT NOT NULL COMMENT '直属领导员工ID',
    application_type VARCHAR(20) NOT NULL COMMENT '类型：LEAVE、OVERTIME',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    status VARCHAR(20) NOT NULL COMMENT '状态：PENDING、APPROVED、REJECTED',
    process_instance_id VARCHAR(64) NULL COMMENT 'Flowable流程实例ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_flow_application_no (application_no),
    UNIQUE KEY uk_flow_process_instance (process_instance_id),
    KEY idx_flow_applicant_created (applicant_id, created_at),
    KEY idx_flow_approver_status (approver_id, status),
    KEY idx_flow_leave_status_time (application_type, status, start_time, end_time)
) ENGINE=InnoDB COMMENT='请假与加班申请表';

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    approver_id BIGINT NOT NULL COMMENT '审批人员工ID',
    action VARCHAR(20) NOT NULL COMMENT '动作：APPROVE、REJECT',
    comment VARCHAR(500) NULL COMMENT '审批意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    KEY idx_approval_application (application_id),
    KEY idx_approval_approver_created (approver_id, created_at)
) ENGINE=InnoDB COMMENT='一级审批记录表';
