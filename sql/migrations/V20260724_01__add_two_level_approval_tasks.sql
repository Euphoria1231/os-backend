-- MySQL 8 migration for oa_flow. Run it with oa_flow selected as the current database.
-- It is safe to rerun: table, columns and indexes are guarded, while data backfill
-- only inserts applications that do not yet have an approval task.

SET @schema_name = DATABASE();

CREATE TABLE IF NOT EXISTS approval_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批任务ID',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    approval_level INT NOT NULL COMMENT '审批级别：1直属领导、2部门负责人',
    approver_id BIGINT NOT NULL COMMENT '审批人员工ID',
    approver_name VARCHAR(100) NOT NULL COMMENT '审批人姓名快照',
    status VARCHAR(20) NOT NULL COMMENT 'WAITING、PENDING、APPROVED、REJECTED、CANCELLED',
    activated_at DATETIME NULL COMMENT '任务激活时间',
    processed_at DATETIME NULL COMMENT '任务处理或取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_approval_task_level (application_id, approval_level),
    UNIQUE KEY uk_approval_task_approver (application_id, approver_id),
    KEY idx_approval_task_todo (approver_id, status, activated_at),
    KEY idx_approval_task_application (application_id, approval_level)
) ENGINE=InnoDB COMMENT='两级审批任务表';

SELECT COUNT(*) INTO @has_task_id_column
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'approval_record'
  AND column_name = 'task_id';
SET @add_task_id_sql = IF(@has_task_id_column = 0,
    'ALTER TABLE approval_record ADD COLUMN task_id BIGINT NULL COMMENT ''审批任务ID'' AFTER application_id',
    'SELECT 1');
PREPARE add_task_id_statement FROM @add_task_id_sql;
EXECUTE add_task_id_statement;
DEALLOCATE PREPARE add_task_id_statement;

SELECT COUNT(*) INTO @has_approval_level_column
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'approval_record'
  AND column_name = 'approval_level';
SET @add_approval_level_sql = IF(@has_approval_level_column = 0,
    'ALTER TABLE approval_record ADD COLUMN approval_level INT NULL COMMENT ''审批级别'' AFTER task_id',
    'SELECT 1');
PREPARE add_approval_level_statement FROM @add_approval_level_sql;
EXECUTE add_approval_level_statement;
DEALLOCATE PREPARE add_approval_level_statement;

INSERT INTO approval_task (
    application_id, approval_level, approver_id, approver_name, status,
    activated_at, processed_at, created_at, updated_at
)
SELECT f.id,
       1,
       f.approver_id,
       CONCAT('员工#', f.approver_id),
       CASE f.status
           WHEN 'PENDING' THEN 'PENDING'
           WHEN 'APPROVED' THEN 'APPROVED'
           WHEN 'REJECTED' THEN 'REJECTED'
           ELSE 'CANCELLED'
       END,
       f.created_at,
       CASE
           WHEN f.status = 'PENDING' THEN NULL
           ELSE COALESCE(history.processed_at, f.updated_at)
       END,
       f.created_at,
       f.updated_at
FROM flow_application f
LEFT JOIN approval_task existing_task
       ON existing_task.application_id = f.id
LEFT JOIN (
    SELECT application_id, MIN(created_at) AS processed_at
    FROM approval_record
    GROUP BY application_id
) history ON history.application_id = f.id
WHERE existing_task.id IS NULL;

UPDATE approval_record r
INNER JOIN approval_task t
        ON t.application_id = r.application_id AND t.approval_level = 1
SET r.task_id = t.id,
    r.approval_level = t.approval_level
WHERE r.task_id IS NULL OR r.approval_level IS NULL;

SELECT COUNT(*) INTO @task_id_is_nullable
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'approval_record'
  AND column_name = 'task_id'
  AND is_nullable = 'YES';
SET @require_task_id_sql = IF(@task_id_is_nullable = 1,
    'ALTER TABLE approval_record MODIFY COLUMN task_id BIGINT NOT NULL COMMENT ''审批任务ID''',
    'SELECT 1');
PREPARE require_task_id_statement FROM @require_task_id_sql;
EXECUTE require_task_id_statement;
DEALLOCATE PREPARE require_task_id_statement;

SELECT COUNT(*) INTO @approval_level_is_nullable
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'approval_record'
  AND column_name = 'approval_level'
  AND is_nullable = 'YES';
SET @require_approval_level_sql = IF(@approval_level_is_nullable = 1,
    'ALTER TABLE approval_record MODIFY COLUMN approval_level INT NOT NULL COMMENT ''审批级别''',
    'SELECT 1');
PREPARE require_approval_level_statement FROM @require_approval_level_sql;
EXECUTE require_approval_level_statement;
DEALLOCATE PREPARE require_approval_level_statement;

SELECT COUNT(*) INTO @has_record_task_index
FROM information_schema.statistics
WHERE table_schema = @schema_name
  AND table_name = 'approval_record'
  AND index_name = 'uk_approval_record_task';
SET @add_record_task_index_sql = IF(@has_record_task_index = 0,
    'ALTER TABLE approval_record ADD UNIQUE KEY uk_approval_record_task (task_id)',
    'SELECT 1');
PREPARE add_record_task_index_statement FROM @add_record_task_index_sql;
EXECUTE add_record_task_index_statement;
DEALLOCATE PREPARE add_record_task_index_statement;
