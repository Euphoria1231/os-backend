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

CREATE TABLE IF NOT EXISTS attendance_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '每日考勤汇总ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    clock_in_time DATETIME NULL COMMENT '上班打卡时间',
    clock_out_time DATETIME NULL COMMENT '下班打卡时间',
    work_hours DECIMAL(5, 2) NOT NULL COMMENT '工作时长（小时）',
    status VARCHAR(20) NOT NULL COMMENT '汇总状态',
    calculation_version INT NOT NULL COMMENT '核算版本',
    calculated_at DATETIME NOT NULL COMMENT '核算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_daily_summary_employee_date (employee_id, work_date),
    KEY idx_daily_summary_work_date_status (work_date, status)
) ENGINE=InnoDB COMMENT='员工每日考勤汇总表';

CREATE TABLE IF NOT EXISTS attendance_monthly_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '月度考勤汇总ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    summary_month DATE NOT NULL COMMENT '汇总月份，保存为当月第一天',
    expected_attendance_days INT NOT NULL COMMENT '应出勤天数',
    actual_attendance_days INT NOT NULL COMMENT '实际出勤天数',
    late_count INT NOT NULL COMMENT '迟到次数',
    early_leave_count INT NOT NULL COMMENT '早退次数',
    absence_count INT NOT NULL COMMENT '旷工次数',
    leave_days INT NOT NULL COMMENT '请假天数',
    total_work_hours DECIMAL(8, 2) NOT NULL COMMENT '总工作时长（小时）',
    calculated_at DATETIME NOT NULL COMMENT '汇总计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_monthly_summary_employee_month (employee_id, summary_month),
    KEY idx_monthly_summary_month (summary_month)
) ENGINE=InnoDB COMMENT='员工月度考勤汇总表';
