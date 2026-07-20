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
    attendance_status VARCHAR(20) NOT NULL COMMENT '状态：NORMAL、LATE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_attendance_employee_date (employee_id, attendance_date),
    KEY idx_attendance_employee_date (employee_id, attendance_date),
    KEY idx_attendance_date_status (attendance_date, attendance_status)
) ENGINE=InnoDB COMMENT='员工每日考勤记录表';
