DROP TABLE IF EXISTS personal_notification;
DROP TABLE IF EXISTS notice_read;
DROP TABLE IF EXISTS notice;

CREATE TABLE personal_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_employee_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    related_business_type VARCHAR(50) NOT NULL,
    related_business_id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_personal_notification_event UNIQUE (event_id)
);

CREATE TABLE notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    search_version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE notice_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notice_read_employee UNIQUE (notice_id, employee_id)
);
