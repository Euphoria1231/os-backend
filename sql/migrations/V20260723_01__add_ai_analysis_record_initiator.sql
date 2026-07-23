-- MySQL 8 migration for oa_ai. It is safe to rerun: each schema change is guarded.
-- Backfill policy: historical rows predate requester auditing and are attributed to
-- the configured legacy audit owner (employee ID 1). Verify that this ID is the
-- designated system audit owner before production execution.

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_initiator_column
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'ai_analysis_record'
  AND column_name = 'initiator_employee_id';
SET @add_initiator_sql = IF(@has_initiator_column = 0,
    'ALTER TABLE ai_analysis_record ADD COLUMN initiator_employee_id BIGINT NULL AFTER business_reference_id',
    'SELECT 1');
PREPARE add_initiator_statement FROM @add_initiator_sql;
EXECUTE add_initiator_statement;
DEALLOCATE PREPARE add_initiator_statement;

UPDATE ai_analysis_record
SET initiator_employee_id = 1
WHERE initiator_employee_id IS NULL OR initiator_employee_id <= 0;

ALTER TABLE ai_analysis_record
    MODIFY COLUMN initiator_employee_id BIGINT NOT NULL COMMENT '发起智能分析的员工ID';

SELECT COUNT(*) INTO @has_initiator_index
FROM information_schema.statistics
WHERE table_schema = @schema_name
  AND table_name = 'ai_analysis_record'
  AND index_name = 'idx_ai_analysis_initiator_audited_at';
SET @add_initiator_index_sql = IF(@has_initiator_index = 0,
    'ALTER TABLE ai_analysis_record ADD KEY idx_ai_analysis_initiator_audited_at (initiator_employee_id, audited_at)',
    'SELECT 1');
PREPARE add_initiator_index_statement FROM @add_initiator_index_sql;
EXECUTE add_initiator_index_statement;
DEALLOCATE PREPARE add_initiator_index_statement;
