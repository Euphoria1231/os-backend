CREATE DATABASE IF NOT EXISTS oa_attendance
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_attendance;

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤记录ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    clock_in_time DATETIME NULL COMMENT '上班打卡时间',
    clock_out_time DATETIME NULL COMMENT '下班打卡时间',
    attendance_status VARCHAR(20) NOT NULL COMMENT '状态：NORMAL、LATE、MAKEUP',
    original_attendance_status VARCHAR(20) NULL COMMENT '补签前原始考勤状态',
    makeup_application_id BIGINT NULL COMMENT '审批通过的补签申请ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_attendance_employee_date (employee_id, attendance_date),
    UNIQUE KEY uk_attendance_makeup_application (makeup_application_id),
    KEY idx_attendance_employee_date (employee_id, attendance_date),
    KEY idx_attendance_date_status (attendance_date, attendance_status)
) ENGINE=InnoDB COMMENT='员工每日考勤记录表';

CREATE TABLE IF NOT EXISTS attendance_makeup_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '补签额度ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    quota_month DATE NOT NULL COMMENT '额度月份，统一保存当月第一天',
    total_count INT NOT NULL COMMENT '当月补签总次数',
    used_count INT NOT NULL DEFAULT 0 COMMENT '当月已使用次数',
    assigned_by BIGINT NOT NULL COMMENT '指定额度的直属领导员工ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_makeup_quota_employee_month (employee_id, quota_month),
    KEY idx_makeup_quota_assigned_by_month (assigned_by, quota_month),
    CONSTRAINT chk_makeup_quota_total_positive CHECK (total_count > 0),
    CONSTRAINT chk_makeup_quota_used_range CHECK (used_count >= 0 AND used_count <= total_count)
) ENGINE=InnoDB COMMENT='员工月度补签额度表';

CREATE TABLE IF NOT EXISTS attendance_makeup_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '补签使用记录ID',
    application_id BIGINT NOT NULL COMMENT '审批申请ID，作为幂等键',
    attendance_record_id BIGINT NOT NULL COMMENT '考勤记录ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    quota_month DATE NOT NULL COMMENT '消耗额度月份，统一保存当月第一天',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_makeup_usage_application (application_id),
    UNIQUE KEY uk_makeup_usage_record (attendance_record_id),
    KEY idx_makeup_usage_employee_month (employee_id, quota_month)
) ENGINE=InnoDB COMMENT='审批通过的补签额度使用记录';
