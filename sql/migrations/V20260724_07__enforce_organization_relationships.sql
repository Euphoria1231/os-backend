-- MySQL 8 migration for oa_user. Run it after init_data.sql.
-- It is safe to rerun: the column and index are guarded, while data repairs
-- only align the current demo organization with its configured leaders.

USE oa_user;

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_position_department_column
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'position'
  AND column_name = 'department_id';
SET @add_position_department_sql = IF(@has_position_department_column = 0,
    'ALTER TABLE position ADD COLUMN department_id BIGINT NULL COMMENT ''所属部门ID'' AFTER id',
    'SELECT 1');
PREPARE add_position_department_statement FROM @add_position_department_sql;
EXECUTE add_position_department_statement;
DEALLOCATE PREPARE add_position_department_statement;

-- test2 follows the approved route: test2 -> 张员工 -> 研发部负责人李主管。
UPDATE employee applicant
INNER JOIN employee direct_leader ON direct_leader.id = applicant.leader_id
SET applicant.department_id = direct_leader.department_id
WHERE applicant.username = 'test2'
  AND applicant.department_id != direct_leader.department_id;

-- Existing positions inherit the only department in which they are currently used.
UPDATE position p
INNER JOIN (
    SELECT position_id, MIN(department_id) AS department_id
    FROM employee
    GROUP BY position_id
    HAVING COUNT(DISTINCT department_id) = 1
) resolved ON resolved.position_id = p.id
SET p.department_id = resolved.department_id
WHERE p.department_id IS NULL
   OR p.department_id != resolved.department_id;

-- Keep unused legacy positions operable; administrators can reassign them later.
UPDATE position
SET department_id = 1
WHERE department_id IS NULL;

-- Platform super administrators do not participate in business approval routes.
UPDATE department d
INNER JOIN employee_role er ON er.employee_id = d.leader_employee_id
INNER JOIN sys_role r ON r.id = er.role_id
SET d.leader_employee_id = NULL
WHERE r.code = 'SUPER_ADMIN';

SELECT COUNT(*) INTO @position_department_is_nullable
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'position'
  AND column_name = 'department_id'
  AND is_nullable = 'YES';
SET @require_position_department_sql = IF(@position_department_is_nullable = 1,
    'ALTER TABLE position MODIFY COLUMN department_id BIGINT NOT NULL DEFAULT 1 COMMENT ''所属部门ID''',
    'SELECT 1');
PREPARE require_position_department_statement FROM @require_position_department_sql;
EXECUTE require_position_department_statement;
DEALLOCATE PREPARE require_position_department_statement;

SELECT COUNT(*) INTO @has_position_department_index
FROM information_schema.statistics
WHERE table_schema = @schema_name
  AND table_name = 'position'
  AND index_name = 'idx_position_department';
SET @add_position_department_index_sql = IF(@has_position_department_index = 0,
    'ALTER TABLE position ADD KEY idx_position_department (department_id)',
    'SELECT 1');
PREPARE add_position_department_index_statement FROM @add_position_department_index_sql;
EXECUTE add_position_department_index_statement;
DEALLOCATE PREPARE add_position_department_index_statement;
