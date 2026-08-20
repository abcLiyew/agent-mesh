-- ============================================
-- Agent Mesh 数据库精简脚本
-- 用途: 删除冗余表和字段,优化数据库结构
-- 执行前请务必备份数据!
-- ============================================

BEGIN;

-- ============================================
-- 1. 删除测试用的向量存储表(这些是Spring AI自动生成的测试表)
-- ============================================
DROP TABLE IF EXISTS ollama_vector_store CASCADE;
DROP TABLE IF EXISTS openai_vector_store CASCADE;
DROP TABLE IF EXISTS dashscope_vector_store CASCADE;
DROP TABLE IF EXISTS vector_store CASCADE;

-- ============================================
-- 2. 删除team相关表(当前版本暂不支持团队功能)
-- ============================================
DROP TABLE IF EXISTS team_resource_share CASCADE;
DROP TABLE IF EXISTS team CASCADE;

-- ============================================
-- 3. 删除技能包相关表(当前版本暂不实现技能市场)
-- ============================================
DROP TABLE IF EXISTS user_skill_installation CASCADE;
DROP TABLE IF EXISTS agent_skill_package CASCADE;

-- ============================================
-- 4. 清理agent表的冗余字段
-- ============================================
-- 删除team_ids和visibility字段(暂不支持团队共享)
ALTER TABLE agent DROP COLUMN IF EXISTS team_ids;
ALTER TABLE agent DROP COLUMN IF EXISTS visibility;

-- 删除tool_schema_json和tool_description(工具配置通过agent_tool_relation管理)
ALTER TABLE agent DROP COLUMN IF EXISTS tool_schema_json;
ALTER TABLE agent DROP COLUMN IF EXISTS tool_description;

-- 删除version字段(版本管理可通过其他方式实现)
ALTER TABLE agent DROP COLUMN IF EXISTS version;

-- 简化model_selection_strategy,使用默认值
ALTER TABLE agent ALTER COLUMN model_selection_strategy SET DEFAULT 'ADAPTIVE';

-- ============================================
-- 5. 清理knowledge_base表的冗余字段
-- ============================================
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS team_ids;
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS visibility;

-- ============================================
-- 6. 清理knowledge_base_document表的冗余字段
-- ============================================
-- related_tool_ids用于RAG驱动的工具推荐,保留但简化
-- 如果不需要可以删除:
-- ALTER TABLE knowledge_base_document DROP COLUMN IF EXISTS related_tool_ids;

-- ============================================
-- 7. 优化user表字段
-- ============================================
-- 删除experience_level和member_expire_at(会员系统暂不实现)
ALTER TABLE "user" DROP COLUMN IF EXISTS experience_level;
ALTER TABLE "user" DROP COLUMN IF EXISTS member_expire_at;

-- 简化user_role注释
COMMENT ON COLUMN "user".user_role IS '用户角色: 0=普通用户, 90=管理员, 99=超级管理员';

-- ============================================
-- 8. 清理model_provider表的冗余字段
-- ============================================
-- is_public预留功能,暂时删除
ALTER TABLE model_provider DROP COLUMN IF EXISTS is_public;

-- ============================================
-- 9. 优化索引(删除不再需要的索引)
-- ============================================
DROP INDEX IF EXISTS idx_user_member;
DROP INDEX IF EXISTS idx_agent_visibility;
DROP INDEX IF EXISTS idx_kb_visibility;

-- ============================================
-- 10. 添加必要的索引(提升查询性能)
-- ============================================
-- 工作流查询优化
CREATE INDEX IF NOT EXISTS idx_workflow_user_enabled 
    ON workflow_definition (user_id, enabled, is_delete);

-- 对话日志查询优化
CREATE INDEX IF NOT EXISTS idx_conv_log_user_session 
    ON conversation_log (user_id, session_id, created_at DESC);

-- 工具调用统计优化
CREATE INDEX IF NOT EXISTS idx_conv_log_tools 
    ON conversation_log USING GIN (invoked_tool_ids) 
    WHERE invoked_tool_ids IS NOT NULL;

-- 记忆检索优化
CREATE INDEX IF NOT EXISTS idx_memory_user_type_active 
    ON agent_long_term_memory (user_id, memory_type, is_active) 
    WHERE is_active = true;

COMMIT;

-- ============================================
-- 验证清理结果
-- ============================================
-- 查看剩余表列表
SELECT tablename 
FROM pg_tables 
WHERE schemaname = 'public' 
ORDER BY tablename;

-- 查看核心表的字段
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'agent' 
ORDER BY ordinal_position;

