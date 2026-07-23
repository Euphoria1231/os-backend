CREATE DATABASE IF NOT EXISTS oa_flow
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_flow;

CREATE TABLE IF NOT EXISTS flow_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    application_no VARCHAR(50) NOT NULL COMMENT '申请单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人员工ID',
    approver_id BIGINT NOT NULL COMMENT '当前有效审批人员工ID',
    application_type VARCHAR(20) NOT NULL COMMENT '类型：LEAVE、OVERTIME、MAKEUP',
    attendance_record_id BIGINT NULL COMMENT '补签关联的考勤记录ID',
    makeup_active_marker TINYINT NULL COMMENT '有效补签申请标记，驳回后置空',
    start_time DATETIME NULL COMMENT '开始时间，补签申请为空',
    end_time DATETIME NULL COMMENT '结束时间，补签申请为空',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    status VARCHAR(20) NOT NULL COMMENT '状态：PENDING、APPROVED、REJECTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_flow_application_no (application_no),
    UNIQUE KEY uk_flow_makeup_active (attendance_record_id, makeup_active_marker),
    KEY idx_flow_applicant_created (applicant_id, created_at),
    KEY idx_flow_approver_status (approver_id, status)
) ENGINE=InnoDB COMMENT='请假、加班与补签申请表';

CREATE TABLE IF NOT EXISTS approval_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批任务ID',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    approval_level INT NOT NULL COMMENT '审批级别：1直属领导、2部门负责人',
    approver_id BIGINT NOT NULL COMMENT '审批人员工ID',
    approver_name VARCHAR(100) NOT NULL COMMENT '审批人姓名快照',
    status VARCHAR(20) NOT NULL COMMENT 'WAITING、PENDING、APPROVED、REJECTED、CANCELLED',
    activated_at DATETIME NULL COMMENT '任务激活时间',
    processed_at DATETIME NULL COMMENT '任务处理或取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_approval_task_level (application_id, approval_level),
    UNIQUE KEY uk_approval_task_approver (application_id, approver_id),
    KEY idx_approval_task_todo (approver_id, status, activated_at),
    KEY idx_approval_task_application (application_id, approval_level)
) ENGINE=InnoDB COMMENT='两级审批任务表';

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    task_id BIGINT NOT NULL COMMENT '审批任务ID',
    approval_level INT NOT NULL COMMENT '审批级别',
    approver_id BIGINT NOT NULL COMMENT '审批人员工ID',
    action VARCHAR(20) NOT NULL COMMENT '动作：APPROVE、REJECT',
    comment VARCHAR(500) NULL COMMENT '审批意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    UNIQUE KEY uk_approval_record_task (task_id),
    KEY idx_approval_application (application_id),
    KEY idx_approval_approver_created (approver_id, created_at)
) ENGINE=InnoDB COMMENT='审批处理记录表';
