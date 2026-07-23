-- MySQL 8 migration for oa_flow. Run it with oa_flow selected as the current database.
-- The version is incremented in the same transaction as each index-affecting write.

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_flow_search_version
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'flow_application'
  AND column_name = 'search_version';
SET @add_flow_search_version_sql = IF(@has_flow_search_version = 0,
    'ALTER TABLE flow_application ADD COLUMN search_version BIGINT NOT NULL DEFAULT 1 COMMENT ''搜索索引事件版本'' AFTER updated_at',
    'SELECT 1');
PREPARE add_flow_search_version_statement FROM @add_flow_search_version_sql;
EXECUTE add_flow_search_version_statement;
DEALLOCATE PREPARE add_flow_search_version_statement;
