-- Demo accounts all use password: Oa@123456
-- The database stores only the BCrypt hash generated with cost factor 10.

USE oa_user;

INSERT INTO department (id, parent_id, name, leader_employee_id, sort_order, status)
VALUES (1, 0, '综合管理部', 1, 1, 1),
       (2, 0, '研发部', 2, 2, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    leader_employee_id = VALUES(leader_employee_id),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO position (id, code, name, description, status)
VALUES (1, 'SYSTEM_ADMIN', '系统管理员', '负责系统配置与权限管理', 1),
       (2, 'DEPARTMENT_MANAGER', '部门主管', '负责团队管理与一级审批', 1),
       (3, 'JAVA_ENGINEER', 'Java 开发工程师', '负责企业应用研发', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO employee (
    id, employee_no, username, password_hash, real_name,
    department_id, position_id, leader_id, phone, email, status
)
VALUES (1, 'OA0001', 'admin',
        '$2b$10$dTFIDeB5Y.77E11ITtw1fuAxyhC5jLDbYtfia50QWmnImuBOROX4S',
        '系统管理员', 1, 1, NULL, '13800000001', 'admin@example.com', 1),
       (2, 'OA0002', 'leader',
        '$2b$10$dTFIDeB5Y.77E11ITtw1fuAxyhC5jLDbYtfia50QWmnImuBOROX4S',
        '李主管', 2, 2, 1, '13800000002', 'leader@example.com', 1),
       (3, 'OA0003', 'employee',
        '$2b$10$dTFIDeB5Y.77E11ITtw1fuAxyhC5jLDbYtfia50QWmnImuBOROX4S',
        '张员工', 2, 3, 2, '13800000003', 'employee@example.com', 1)
ON DUPLICATE KEY UPDATE
    employee_no = VALUES(employee_no),
    password_hash = VALUES(password_hash),
    real_name = VALUES(real_name),
    department_id = VALUES(department_id),
    position_id = VALUES(position_id),
    leader_id = VALUES(leader_id),
    phone = VALUES(phone),
    email = VALUES(email),
    status = VALUES(status);

INSERT INTO sys_role (id, code, name, status)
VALUES (1, 'SUPER_ADMIN', '超级管理员', 1),
       (2, 'EMPLOYEE', '普通员工', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    status = VALUES(status);

INSERT INTO sys_menu (
    id, parent_id, name, path, component, permission, type, sort_order, status
)
VALUES (1, 0, '系统管理', '/system', NULL, NULL, 'DIRECTORY', 1, 1),
       (2, 1, '组织与权限', '/system/organization', 'system/organization/index',
        'system:organization:view', 'MENU', 1, 1),
       (3, 0, '考勤管理', '/attendance', 'attendance/index',
        'attendance:view', 'MENU', 2, 1),
       (4, 0, '审批管理', '/flow', 'flow/index',
        'flow:view', 'MENU', 3, 1),
       (5, 0, '公告通知', '/notices', 'notices/index',
        'notice:view', 'MENU', 4, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    path = VALUES(path),
    component = VALUES(component),
    permission = VALUES(permission),
    type = VALUES(type),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO sys_api_permission (id, code, name, http_method, path_pattern, status)
VALUES (1, 'USER_READ', '查询用户与权限数据', 'GET', '/api/user/**', 1),
       (2, 'USER_CREATE', '新增用户与权限数据', 'POST', '/api/user/**', 1),
       (3, 'USER_UPDATE', '修改用户与权限数据', 'PUT', '/api/user/**', 1),
       (4, 'USER_DELETE', '删除用户与权限数据', 'DELETE', '/api/user/**', 1),
       (5, 'ATTENDANCE_READ', '查询个人考勤', 'GET', '/api/attendance/**', 1),
       (6, 'ATTENDANCE_CLOCK', '上下班打卡', 'POST', '/api/attendance/**', 1),
       (7, 'FLOW_APPLICATION_READ', '查询个人申请', 'GET', '/api/flow/applications/**', 1),
       (8, 'FLOW_APPLICATION_SUBMIT', '提交请假或加班申请',
        'POST', '/api/flow/applications/**', 1),
       (9, 'FLOW_TASK_READ', '查询待办与已办', 'GET', '/api/flow/tasks/**', 1),
       (10, 'FLOW_TASK_APPROVE', '审批申请', 'POST', '/api/flow/tasks/**', 1),
       (11, 'NOTICE_READ', '查看公告', 'GET', '/api/notices/**', 1),
       (12, 'NOTICE_MARK_READ', '标记公告已读', 'PUT', '/api/notices/*/read', 1),
       (13, 'NOTICE_PUBLISH', '发布公告', 'POST', '/api/notices', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status);

INSERT IGNORE INTO employee_role (employee_id, role_id)
VALUES (1, 1),
       (2, 2),
       (3, 2);

INSERT IGNORE INTO role_menu (role_id, menu_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
       (2, 3), (2, 4), (2, 5);

INSERT IGNORE INTO role_api_permission (role_id, api_permission_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
       (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13),
       (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11), (2, 12);

USE oa_notice;

INSERT INTO notice (id, title, content, publisher_id, status, published_at)
VALUES (1, '欢迎使用 OA 办公管理系统',
        '系统基础功能已经就绪，请使用演示账号完成考勤、审批和公告流程。',
        1, 'PUBLISHED', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    publisher_id = VALUES(publisher_id),
    status = VALUES(status);
