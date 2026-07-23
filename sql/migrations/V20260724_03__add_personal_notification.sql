USE oa_notice;

CREATE TABLE IF NOT EXISTS personal_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '个人通知ID',
    recipient_employee_id BIGINT NOT NULL COMMENT '接收人员工ID',
    notification_type VARCHAR(50) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    related_business_type VARCHAR(50) NOT NULL COMMENT '关联业务类型',
    related_business_id BIGINT NOT NULL COMMENT '关联业务ID',
    event_id VARCHAR(64) NOT NULL COMMENT '来源事件幂等ID',
    read_at DATETIME NULL COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_personal_notification_event (event_id),
    KEY idx_personal_notification_recipient (recipient_employee_id, read_at, created_at)
) ENGINE=InnoDB COMMENT='个人站内通知表';
