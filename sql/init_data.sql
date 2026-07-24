-- Active: 1784102221766@@127.0.0.1@3306@oa_user
-- Demo accounts all use password: Oa@123456
-- The database stores only the BCrypt hash generated with cost factor 10.

USE oa_user;

INSERT INTO department (id, parent_id, name, leader_employee_id, sort_order, status)
VALUES (1, 0, '综合管理部', NULL, 1, 1),
       (2, 0, '研发部', 2, 2, 1) AS incoming
ON DUPLICATE KEY UPDATE
    parent_id = incoming.parent_id,
    leader_employee_id = incoming.leader_employee_id,
    sort_order = incoming.sort_order,
    status = incoming.status;

INSERT INTO `position` (id, department_id, code, name, description, status)
VALUES (1, 1, 'SYSTEM_ADMIN', '系统管理员', '负责系统配置与权限管理', 1),
       (2, 2, 'DEPARTMENT_MANAGER', '部门主管', '负责团队管理与一级审批', 1),
       (3, 2, 'JAVA_ENGINEER', 'Java 开发工程师', '负责企业应用研发', 1) AS incoming
ON DUPLICATE KEY UPDATE
    department_id = incoming.department_id,
    name = incoming.name,
    description = incoming.description,
    status = incoming.status;

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
        '张员工', 2, 3, 2, '13800000003', 'employee@example.com', 1) AS incoming
ON DUPLICATE KEY UPDATE
    employee_no = incoming.employee_no,
    password_hash = incoming.password_hash,
    real_name = incoming.real_name,
    department_id = incoming.department_id,
    position_id = incoming.position_id,
    leader_id = incoming.leader_id,
    phone = incoming.phone,
    email = incoming.email,
    status = incoming.status;

INSERT INTO sys_role (id, code, name, status)
VALUES (1, 'SUPER_ADMIN', '超级管理员', 1),
       (2, 'EMPLOYEE', '普通员工', 1),
       (3, 'DEPARTMENT_MANAGER', '部门主管', 1) AS incoming
ON DUPLICATE KEY UPDATE
    name = incoming.name,
    status = incoming.status;

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
        'notice:view', 'MENU', 4, 1) AS incoming
ON DUPLICATE KEY UPDATE
    parent_id = incoming.parent_id,
    name = incoming.name,
    path = incoming.path,
    component = incoming.component,
    permission = incoming.permission,
    type = incoming.type,
    sort_order = incoming.sort_order,
    status = incoming.status;

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
       (13, 'NOTICE_PUBLISH', '发布公告', 'POST', '/api/notices', 1),
       (14, 'INTELLIGENCE_SEARCH_READ', '使用全文检索',
        'GET', '/api/intelligence/search/**', 1),
       (15, 'USER_DIRECT_REPORT_READ', '查询直属员工',
        'GET', '/api/user/employees/direct-reports', 1),
       (16, 'ATTENDANCE_MAKEUP_QUOTA_ASSIGN', '配置直属员工补签额度',
        'PUT', '/api/attendance/makeup-quotas/**', 1),
       (17, 'INTELLIGENCE_AI_USE', '使用智能办公分析',
        'POST', '/api/intelligence/ai/**', 1),
       (18, 'INTELLIGENCE_AI_RECORD_READ', '查询智能分析审计记录',
        'GET', '/api/intelligence/ai/**', 1) AS incoming
ON DUPLICATE KEY UPDATE
    name = incoming.name,
    http_method = incoming.http_method,
    path_pattern = incoming.path_pattern,
    status = incoming.status;

DELETE FROM employee_role WHERE employee_id IN (1, 2, 3);

INSERT INTO employee_role (employee_id, role_id)
VALUES (1, 1),
       (2, 3),
       (3, 2) AS incoming
ON DUPLICATE KEY UPDATE
    employee_id = incoming.employee_id;

INSERT INTO role_menu (role_id, menu_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
       (2, 3), (2, 4), (2, 5),
       (3, 3), (3, 4), (3, 5) AS incoming
ON DUPLICATE KEY UPDATE
    role_id = incoming.role_id;

DELETE FROM role_api_permission
WHERE role_id = 2
  AND api_permission_id IN (9, 10);

INSERT INTO role_api_permission (role_id, api_permission_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
       (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13),
       (1, 15), (1, 16), (1, 17), (1, 18),
       (2, 5), (2, 6), (2, 7), (2, 8), (2, 11), (2, 12), (2, 14), (2, 17), (2, 18),
       (3, 5), (3, 6), (3, 7), (3, 8), (3, 9), (3, 10), (3, 11), (3, 12), (3, 14),
       (3, 15), (3, 16), (3, 17), (3, 18) AS incoming
ON DUPLICATE KEY UPDATE
    role_id = incoming.role_id;

INSERT IGNORE INTO role_api_permission (role_id, api_permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_api_permission p
WHERE r.code = 'EMPLOYEE'
  AND p.code IN ('FLOW_TASK_READ', 'FLOW_TASK_APPROVE');

USE oa_notice;

INSERT INTO notice (id, title, content, publisher_id, status, published_at)
VALUES (1, '欢迎使用 OA 办公管理系统',
        '系统基础功能已经就绪，请使用演示账号完成考勤、审批和公告流程。',
        1, 'PUBLISHED', CURRENT_TIMESTAMP) AS incoming
ON DUPLICATE KEY UPDATE
    title = incoming.title,
    content = incoming.content,
    publisher_id = incoming.publisher_id,
    status = incoming.status;
