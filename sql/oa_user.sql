CREATE DATABASE IF NOT EXISTS oa_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE oa_user;

CREATE TABLE IF NOT EXISTS department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级部门ID，0表示根部门',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    leader_employee_id BIGINT NULL COMMENT '部门负责人员工ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_department_name (name),
    KEY idx_department_parent (parent_id),
    KEY idx_department_status (status)
) ENGINE=InnoDB COMMENT='部门表';
