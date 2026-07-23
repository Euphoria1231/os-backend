-- MySQL 8 migration for oa_ai. It is safe to rerun: each schema change is guarded.
-- Legacy policy: historical rows are intentionally unattributed (NULL). They must
-- be visible only to SUPER_ADMIN; Java invariants require every new write to have
-- a positive initiator employee ID.

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_initiator_column
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'ai_analysis_record'
  AND column_name = 'initiator_employee_id';
SET @add_initiator_sql = IF(@has_initiator_column = 0,
    'ALTER TABLE ai_analysis_record ADD COLUMN initiator_employee_id BIGINT NULL COMMENT ''发起智能分析的员工ID；历史无归属记录仅SUPER_ADMIN可查'' AFTER business_reference_id',
    'SELECT 1');
PREPARE add_initiator_statement FROM @add_initiator_sql;
EXECUTE add_initiator_statement;
DEALLOCATE PREPARE add_initiator_statement;

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
