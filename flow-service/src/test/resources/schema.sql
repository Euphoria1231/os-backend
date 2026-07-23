DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS approval_task;
DROP TABLE IF EXISTS flow_application;

CREATE TABLE flow_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    application_type VARCHAR(20) NOT NULL,
    attendance_record_id BIGINT NULL,
    makeup_active_marker INT NULL,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_application_no UNIQUE (application_no),
    CONSTRAINT uk_flow_makeup_active UNIQUE (attendance_record_id, makeup_active_marker)
);

CREATE TABLE approval_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    approval_level INT NOT NULL,
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    activated_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_approval_task_level UNIQUE (application_id, approval_level),
    CONSTRAINT uk_approval_task_approver UNIQUE (application_id, approver_id)
);

CREATE TABLE approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    approval_level INT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_approval_record_task UNIQUE (task_id)
);
