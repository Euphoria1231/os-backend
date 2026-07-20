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
