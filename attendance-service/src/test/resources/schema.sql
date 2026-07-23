DROP TABLE IF EXISTS attendance_makeup_quota;
DROP TABLE IF EXISTS attendance_record;

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

CREATE TABLE attendance_makeup_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    quota_month DATE NOT NULL,
    total_count INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    assigned_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_makeup_quota_employee_month UNIQUE (employee_id, quota_month),
    CONSTRAINT chk_makeup_quota_total_positive CHECK (total_count > 0),
    CONSTRAINT chk_makeup_quota_used_range CHECK (used_count >= 0 AND used_count <= total_count)
);
