DROP TABLE IF EXISTS role_api_permission;
DROP TABLE IF EXISTS role_menu;
DROP TABLE IF EXISTS employee_role;
DROP TABLE IF EXISTS sys_api_permission;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS business_operation_log;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS position;
DROP TABLE IF EXISTS department;

CREATE TABLE department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    leader_employee_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_department_name UNIQUE (name)
);

CREATE TABLE position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_position_code UNIQUE (code)
);

CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_no VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    leader_id BIGINT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(100) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employee_no UNIQUE (employee_no),
    CONSTRAINT uk_employee_username UNIQUE (username)
);

CREATE TABLE business_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NOT NULL,
    service_name VARCHAR(50) NOT NULL,
    business_module VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id VARCHAR(100) NULL,
    summary VARCHAR(500) NOT NULL,
    operation_status VARCHAR(20) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    client_ip VARCHAR(64) NULL,
    error_message VARCHAR(500) NULL,
    operated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(200),
    permission VARCHAR(100),
    type VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_api_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path_pattern VARCHAR(200) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_api_permission_code UNIQUE (code)
);

CREATE TABLE employee_role (
    employee_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (employee_id, role_id)
);

CREATE TABLE role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE role_api_permission (
    role_id BIGINT NOT NULL,
    api_permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, api_permission_id)
);
