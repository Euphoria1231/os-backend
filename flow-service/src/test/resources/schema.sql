DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS flow_application;

CREATE TABLE flow_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    application_type VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    process_instance_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_application_no UNIQUE (application_no)
);

CREATE TABLE approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
