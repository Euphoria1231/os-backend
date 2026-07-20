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

CREATE TABLE IF NOT EXISTS position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    code VARCHAR(50) NOT NULL COMMENT '岗位编码',
    name VARCHAR(100) NOT NULL COMMENT '岗位名称',
    description VARCHAR(500) NULL COMMENT '岗位说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_position_code (code),
    KEY idx_position_status (status)
) ENGINE=InnoDB COMMENT='岗位表';

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    employee_no VARCHAR(50) NOT NULL COMMENT '员工编号',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码Hash',
    real_name VARCHAR(100) NOT NULL COMMENT '员工姓名',
    department_id BIGINT NOT NULL COMMENT '部门ID',
    position_id BIGINT NOT NULL COMMENT '岗位ID',
    leader_id BIGINT NULL COMMENT '直属领导员工ID',
    phone VARCHAR(30) NULL COMMENT '联系电话',
    email VARCHAR(100) NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_no (employee_no),
    UNIQUE KEY uk_employee_username (username),
    KEY idx_employee_department (department_id),
    KEY idx_employee_position (position_id),
    KEY idx_employee_leader (leader_id),
    KEY idx_employee_status (status)
) ENGINE=InnoDB COMMENT='员工表';
