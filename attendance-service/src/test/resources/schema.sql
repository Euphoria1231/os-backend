DROP TABLE IF EXISTS attendance_record;
DROP TABLE IF EXISTS attendance_daily_summary;

CREATE TABLE attendance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    clock_in_time TIMESTAMP NULL,
    clock_out_time TIMESTAMP NULL,
    attendance_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_employee_date UNIQUE (employee_id, attendance_date)
);

CREATE TABLE attendance_daily_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    clock_in_time TIMESTAMP NULL,
    clock_out_time TIMESTAMP NULL,
    work_hours DECIMAL(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    calculation_version INT NOT NULL,
    calculated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_summary_employee_date UNIQUE (employee_id, work_date)
);
