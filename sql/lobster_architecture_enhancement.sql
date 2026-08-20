-- ============================================
-- 龙虾架构功能完善 - 数据库补充脚本
-- 用途: 添加经验数据库、长期记忆表、技能市场等表
-- ============================================

BEGIN;

-- ============================================
-- 1. 执行经验数据库表 (Execution Experience Database)
-- 用于记录工作流/任务的执行经验,支持学习和优化
-- ============================================
CREATE TABLE IF NOT EXISTS execution_experience (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT,
    experience_type VARCHAR(50) NOT NULL,  -- workflow_execution, task_planning, tool_invocation
    success BOOLEAN NOT NULL,
    rating SMALLINT,  -- 1-5 评分
    user_feedback TEXT,
    execution_time_ms BIGINT,
    decision_path_json JSONB,  -- 决策路径快照
    context_summary TEXT,  -- 上下文摘要
    learned_patterns JSONB,  -- 学习到的模式
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

COMMENT ON TABLE execution_experience IS '执行经验数据库表 - 用于龙虾架构的学习和优化';
COMMENT ON COLUMN execution_experience.workflow_id IS '关联的工作流ID';
COMMENT ON COLUMN execution_experience.experience_type IS '经验类型: workflow_execution, task_planning, tool_invocation';
COMMENT ON COLUMN execution_experience.success IS '执行是否成功';
COMMENT ON COLUMN execution_experience.rating IS '用户评分 1-5';
COMMENT ON COLUMN execution_experience.user_feedback IS '用户反馈文本';
COMMENT ON COLUMN execution_experience.execution_time_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN execution_experience.decision_path_json IS '决策路径JSON快照';
COMMENT ON COLUMN execution_experience.context_summary IS '上下文摘要';
COMMENT ON COLUMN execution_experience.learned_patterns IS '学习到的模式(JSON格式)';

CREATE INDEX idx_exp_workflow_id ON execution_experience(workflow_id);
CREATE INDEX idx_exp_type_success ON execution_experience(experience_type, success);
CREATE INDEX idx_exp_created_at ON execution_experience(created_at DESC);

-- ============================================
-- 2. 智能体长期记忆表 (Agent Long-term Memory)
-- 存储用户的偏好、历史交互模式等长期记忆
-- ============================================
CREATE TABLE IF NOT EXISTS agent_long_term_memory (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_type VARCHAR(50) NOT NULL,  -- preference, interaction_pattern, domain_knowledge, feedback_insight
    memory_key VARCHAR(200) NOT NULL,  -- 记忆键,用于快速检索
    memory_value TEXT,  -- 记忆值
    memory_vector VECTOR(1536),  -- 记忆向量,用于相似度检索
    confidence_score NUMERIC(3, 2) DEFAULT 0.5,  -- 置信度 0-1
    usage_count INTEGER DEFAULT 0,  -- 使用次数
    last_used_at TIMESTAMP,  -- 最后使用时间
    source_type VARCHAR(50),  -- 来源类型: explicit_feedback, implicit_observation, llm_extraction
    source_reference_id BIGINT,  -- 来源引用ID (如 conversation_log.id)
    tags JSONB,  -- 标签数组
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,  -- 过期时间,NULL表示永久有效
    UNIQUE (user_id, memory_type, memory_key)
);

COMMENT ON TABLE agent_long_term_memory IS '智能体长期记忆表 - 存储用户偏好和交互模式';
COMMENT ON COLUMN agent_long_term_memory.memory_type IS '记忆类型: preference, interaction_pattern, domain_knowledge, feedback_insight';
COMMENT ON COLUMN agent_long_term_memory.memory_key IS '记忆键,用于快速检索';
COMMENT ON COLUMN agent_long_term_memory.memory_value IS '记忆值';
COMMENT ON COLUMN agent_long_term_memory.memory_vector IS '记忆向量,用于相似度检索';
COMMENT ON COLUMN agent_long_term_memory.confidence_score IS '置信度 0-1';
COMMENT ON COLUMN agent_long_term_memory.usage_count IS '使用次数';
COMMENT ON COLUMN agent_long_term_memory.last_used_at IS '最后使用时间';
COMMENT ON COLUMN agent_long_term_memory.source_type IS '来源类型: explicit_feedback, implicit_observation, llm_extraction';
COMMENT ON COLUMN agent_long_term_memory.source_reference_id IS '来源引用ID';
COMMENT ON COLUMN agent_long_term_memory.tags IS '标签数组';
COMMENT ON COLUMN agent_long_term_memory.expires_at IS '过期时间,NULL表示永久有效';

CREATE INDEX idx_memory_user_type ON agent_long_term_memory(user_id, memory_type);
CREATE INDEX idx_memory_user_active ON agent_long_term_memory(user_id, is_active) WHERE is_active = true;
CREATE INDEX idx_memory_vector ON agent_long_term_memory USING hnsw (memory_vector vector_cosine_ops);
CREATE INDEX idx_memory_key ON agent_long_term_memory(memory_key);
CREATE INDEX idx_memory_tags ON agent_long_term_memory USING gin (tags);

-- ============================================
-- 3. 技能包定义表 (Skill Package)
-- 预定义的技能模板,可被多个智能体复用
-- ============================================
CREATE TABLE IF NOT EXISTS agent_skill_package (
    id BIGSERIAL PRIMARY KEY,
    skill_name VARCHAR(200) NOT NULL,
    skill_code VARCHAR(100) NOT NULL UNIQUE,  -- 唯一技能代码
    description TEXT,
    category VARCHAR(50),  -- 分类: data_analysis, content_generation, automation, integration
    version VARCHAR(20) DEFAULT '1.0.0',
    author_id BIGINT,  -- 作者用户ID
    skill_config_json JSONB NOT NULL,  -- 技能配置(包含工具列表、参数模板等)
    input_schema_json JSONB,  -- 输入参数Schema
    output_schema_json JSONB,  -- 输出结果Schema
    example_usage TEXT,  -- 使用示例
    icon_url VARCHAR(500),  -- 图标URL
    download_count INTEGER DEFAULT 0,  -- 下载次数
    rating_avg NUMERIC(3, 2) DEFAULT 0,  -- 平均评分
    rating_count INTEGER DEFAULT 0,  -- 评分次数
    status SMALLINT DEFAULT 1,  -- 1=发布, 0=草稿, -1=下架
    is_public BOOLEAN DEFAULT FALSE,  -- 是否公开共享
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_delete SMALLINT DEFAULT 0
);

COMMENT ON TABLE agent_skill_package IS '技能包定义表 - 可复用的技能模板';
COMMENT ON COLUMN agent_skill_package.skill_code IS '唯一技能代码,用于标识技能';
COMMENT ON COLUMN agent_skill_package.category IS '技能分类';
COMMENT ON COLUMN agent_skill_package.skill_config_json IS '技能配置JSON(包含工具列表、参数模板等)';
COMMENT ON COLUMN agent_skill_package.input_schema_json IS '输入参数Schema';
COMMENT ON COLUMN agent_skill_package.output_schema_json IS '输出结果Schema';
COMMENT ON COLUMN agent_skill_package.example_usage IS '使用示例';
COMMENT ON COLUMN agent_skill_package.download_count IS '下载次数';
COMMENT ON COLUMN agent_skill_package.rating_avg IS '平均评分';
COMMENT ON COLUMN agent_skill_package.rating_count IS '评分次数';
COMMENT ON COLUMN agent_skill_package.is_public IS '是否公开共享';

CREATE INDEX idx_skill_category ON agent_skill_package(category, status);
CREATE INDEX idx_skill_author ON agent_skill_package(author_id);
CREATE INDEX idx_skill_public_status ON agent_skill_package(is_public, status) WHERE is_public = true AND status = 1;

-- ============================================
-- 4. 用户技能安装表 (User Skill Installation)
-- 记录用户安装/订阅的技能
-- ============================================
CREATE TABLE IF NOT EXISTS user_skill_installation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    installation_config_json JSONB,  -- 用户自定义的安装配置
    status SMALLINT DEFAULT 1,  -- 1=已安装, 0=已禁用
    installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, skill_id),
    FOREIGN KEY (skill_id) REFERENCES agent_skill_package(id)
);

COMMENT ON TABLE user_skill_installation IS '用户技能安装表 - 记录用户安装的皮肤';
COMMENT ON COLUMN user_skill_installation.installation_config_json IS '用户自定义的安装配置';
COMMENT ON COLUMN user_skill_installation.status IS '状态: 1=已安装, 0=已禁用';

CREATE INDEX idx_user_skill_user ON user_skill_installation(user_id, status);
CREATE INDEX idx_user_skill_skill ON user_skill_installation(skill_id);

-- ============================================
-- 5. 智能体-技能关联表 (Agent-Skill Relation)
-- 智能体可以使用的技能列表
-- ============================================
CREATE TABLE IF NOT EXISTS agent_skill_relation (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    config_override_json JSONB,  -- 智能体级别的配置覆写
    priority INTEGER DEFAULT 0,  -- 优先级
    sort_order INTEGER DEFAULT 0,  -- 排序顺序
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agent_id, skill_id),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES agent_skill_package(id)
);

COMMENT ON TABLE agent_skill_relation IS '智能体-技能关联表';
COMMENT ON COLUMN agent_skill_relation.config_override_json IS '智能体级别的配置覆写';
COMMENT ON COLUMN agent_skill_relation.priority IS '优先级';
COMMENT ON COLUMN agent_skill_relation.sort_order IS '排序顺序';
COMMENT ON COLUMN agent_skill_relation.is_enabled IS '是否启用';

CREATE INDEX idx_agent_skill_agent ON agent_skill_relation(agent_id, is_enabled);
CREATE INDEX idx_agent_skill_skill ON agent_skill_relation(skill_id);

-- ============================================
-- 6. 工作流模板市场表 (Workflow Template Market)
-- 可共享的工作流模板
-- ============================================
CREATE TABLE IF NOT EXISTS workflow_template_market (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50),  -- 分类: data_processing, content_creation, automation, analysis
    workflow_definition_id BIGINT,  -- 关联的工作流定义ID
    template_config_json JSONB NOT NULL,  -- 模板配置(脱敏后的工作流定义)
    input_parameters_json JSONB,  -- 输入参数说明
    output_description TEXT,  -- 输出说明
    use_cases TEXT,  -- 适用场景
    author_id BIGINT,  -- 作者ID
    download_count INTEGER DEFAULT 0,
    rating_avg NUMERIC(3, 2) DEFAULT 0,
    rating_count INTEGER DEFAULT 0,
    is_public BOOLEAN DEFAULT FALSE,
    price NUMERIC(10, 2) DEFAULT 0,  -- 价格(预留)
    status SMALLINT DEFAULT 1,  -- 1=发布, 0=草稿, -1=下架
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_delete SMALLINT DEFAULT 0
);

COMMENT ON TABLE workflow_template_market IS '工作流模板市场表 - 可共享的工作流模板';
COMMENT ON COLUMN workflow_template_market.template_code IS '唯一模板代码';
COMMENT ON COLUMN workflow_template_market.category IS '模板分类';
COMMENT ON COLUMN workflow_template_market.workflow_definition_id IS '关联的工作流定义ID';
COMMENT ON COLUMN workflow_template_market.template_config_json IS '模板配置(脱敏后的工作流定义)';
COMMENT ON COLUMN workflow_template_market.input_parameters_json IS '输入参数说明';
COMMENT ON COLUMN workflow_template_market.use_cases IS '适用场景';
COMMENT ON COLUMN workflow_template_market.price IS '价格(预留功能)';

CREATE INDEX idx_template_category ON workflow_template_market(category, status);
CREATE INDEX idx_template_author ON workflow_template_market(author_id);
CREATE INDEX idx_template_public ON workflow_template_market(is_public, status) WHERE is_public = true AND status = 1;

-- ============================================
-- 7. 用户模板安装表 (User Template Installation)
-- 记录用户安装的工作流模板
-- ============================================
CREATE TABLE IF NOT EXISTS user_template_installation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    installed_workflow_id BIGINT,  -- 安装后生成的工作流定义ID
    installation_config_json JSONB,
    status SMALLINT DEFAULT 1,
    installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, template_id),
    FOREIGN KEY (template_id) REFERENCES workflow_template_market(id)
);

COMMENT ON TABLE user_template_installation IS '用户模板安装表';
COMMENT ON COLUMN user_template_installation.installed_workflow_id IS '安装后生成的工作流定义ID';

CREATE INDEX idx_user_template_user ON user_template_installation(user_id);
CREATE INDEX idx_user_template_template ON user_template_installation(template_id);

-- ============================================
-- 8. 增强智能体依赖关系字段
-- ============================================
-- 为agent_dependency表添加更多字段
ALTER TABLE agent_dependency ADD COLUMN IF NOT EXISTS condition_expression TEXT;  -- 调用条件表达式
ALTER TABLE agent_dependency ADD COLUMN IF NOT EXISTS parameter_mapping_json JSONB;  -- 参数映射配置
ALTER TABLE agent_dependency ADD COLUMN IF NOT EXISTS timeout_ms INTEGER DEFAULT 30000;  -- 超时时间
ALTER TABLE agent_dependency ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;  -- 重试次数
ALTER TABLE agent_dependency ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;  -- 是否启用

COMMENT ON COLUMN agent_dependency.condition_expression IS '调用条件表达式(SpEL或JavaScript)';
COMMENT ON COLUMN agent_dependency.parameter_mapping_json IS '参数映射配置JSON';
COMMENT ON COLUMN agent_dependency.timeout_ms IS '调用超时时间(毫秒)';
COMMENT ON COLUMN agent_dependency.retry_count IS '失败重试次数';
COMMENT ON COLUMN agent_dependency.is_enabled IS '是否启用该依赖';

-- ============================================
-- 9. 添加对话日志的经验标记字段
-- ============================================
ALTER TABLE conversation_log ADD COLUMN IF NOT EXISTS experience_recorded BOOLEAN DEFAULT FALSE;
ALTER TABLE conversation_log ADD COLUMN IF NOT EXISTS learning_applied BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN conversation_log.experience_recorded IS '是否已记录到经验数据库';
COMMENT ON COLUMN conversation_log.learning_applied IS '是否应用了学习优化';

CREATE INDEX idx_conv_log_experience ON conversation_log(experience_recorded) WHERE experience_recorded = false;

COMMIT;

-- ============================================
-- 验证创建结果
-- ============================================
SELECT tablename 
FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename LIKE '%experience%' 
     OR tablename LIKE '%memory%' 
     OR tablename LIKE '%skill%' 
     OR tablename LIKE '%template%'
ORDER BY tablename;
