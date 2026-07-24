-- MySQL 8 migration for oa_user. Run it after init_data.sql.
-- Approval task ownership is enforced by flow-service, so every active employee
-- may reach the task endpoints without gaining access to another employee's tasks.

USE oa_user;

INSERT IGNORE INTO role_api_permission (role_id, api_permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_api_permission p
WHERE r.code = 'EMPLOYEE'
  AND p.code IN ('FLOW_TASK_READ', 'FLOW_TASK_APPROVE');
