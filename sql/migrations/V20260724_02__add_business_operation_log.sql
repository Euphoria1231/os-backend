-- MySQL 8 migration for oa_user. Run it with oa_user selected as the current database.

CREATE TABLE IF NOT EXISTS business_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '业务操作日志ID',
    operator_id BIGINT NULL COMMENT '操作人员工ID，无法识别的登录失败可为空',
    operator_name VARCHAR(100) NOT NULL COMMENT '操作人名称快照',
    service_name VARCHAR(50) NOT NULL COMMENT '来源服务',
    business_module VARCHAR(50) NOT NULL COMMENT '业务模块',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) NULL COMMENT '目标业务类型',
    target_id VARCHAR(100) NULL COMMENT '目标业务ID',
    summary VARCHAR(500) NOT NULL COMMENT '脱敏后的操作摘要',
    operation_status VARCHAR(20) NOT NULL COMMENT 'SUCCESS或FAILURE',
    request_path VARCHAR(255) NOT NULL COMMENT '不含查询参数的请求路径',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP Method',
    client_ip VARCHAR(64) NULL COMMENT '客户端IP',
    error_message VARCHAR(500) NULL COMMENT '脱敏后的错误摘要',
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_operation_log_operator_time (operator_id, operated_at),
    KEY idx_operation_log_module_status_time (business_module, operation_status, operated_at),
    KEY idx_operation_log_operated_at (operated_at)
) ENGINE=InnoDB COMMENT='跨服务业务操作日志表';
