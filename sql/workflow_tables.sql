-- 工作流定义表
CREATE TABLE workflow_definition (
    id BIGSERIAL PRIMARY KEY,
    workflow_name VARCHAR(200) NOT NULL,
    description TEXT,
    agent_id BIGINT,
    version VARCHAR(50) DEFAULT '1.0',
    nodes_json JSONB NOT NULL,
    start_node_id VARCHAR(100),
    global_variables_json JSONB,
    timeout_ms BIGINT DEFAULT 60000,
    enabled BOOLEAN DEFAULT TRUE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_delete SMALLINT DEFAULT 0
);

COMMENT ON TABLE workflow_definition IS '工作流定义表';
COMMENT ON COLUMN workflow_definition.id IS '主键ID';
COMMENT ON COLUMN workflow_definition.workflow_name IS '工作流名称';
COMMENT ON COLUMN workflow_definition.description IS '工作流描述';
COMMENT ON COLUMN workflow_definition.agent_id IS '关联的智能体ID';
COMMENT ON COLUMN workflow_definition.version IS '工作流版本';
COMMENT ON COLUMN workflow_definition.nodes_json IS '节点定义(JSON格式)';
COMMENT ON COLUMN workflow_definition.start_node_id IS '起始节点ID';
COMMENT ON COLUMN workflow_definition.global_variables_json IS '全局变量(JSON格式)';
COMMENT ON COLUMN workflow_definition.timeout_ms IS '超时时间(毫秒)';
COMMENT ON COLUMN workflow_definition.enabled IS '是否启用';
COMMENT ON COLUMN workflow_definition.user_id IS '创建者用户ID';

-- 工作流执行历史表
CREATE TABLE workflow_execution_history (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL UNIQUE,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    input_params_json JSONB,
    output_result_json JSONB,
    execution_path_json JSONB,
    node_results_json JSONB,
    success BOOLEAN,
    error_message TEXT,
    total_duration_ms BIGINT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (workflow_id) REFERENCES workflow_definition(id)
);

COMMENT ON TABLE workflow_execution_history IS '工作流执行历史表';
COMMENT ON COLUMN workflow_execution_history.execution_id IS '执行ID';
COMMENT ON COLUMN workflow_execution_history.workflow_id IS '工作流ID';
COMMENT ON COLUMN workflow_execution_history.user_id IS '用户ID';
COMMENT ON COLUMN workflow_execution_history.input_params_json IS '输入参数(JSON)';
COMMENT ON COLUMN workflow_execution_history.output_result_json IS '输出结果(JSON)';
COMMENT ON COLUMN workflow_execution_history.execution_path_json IS '执行路径(JSON)';
COMMENT ON COLUMN workflow_execution_history.node_results_json IS '节点执行结果(JSON)';
COMMENT ON COLUMN workflow_execution_history.success IS '是否成功';
COMMENT ON COLUMN workflow_execution_history.error_message IS '错误信息';
COMMENT ON COLUMN workflow_execution_history.total_duration_ms IS '总耗时(毫秒)';

-- 创建索引
CREATE INDEX idx_workflow_user_id ON workflow_definition(user_id);
CREATE INDEX idx_workflow_agent_id ON workflow_definition(agent_id);
CREATE INDEX idx_execution_workflow_id ON workflow_execution_history(workflow_id);
CREATE INDEX idx_execution_user_id ON workflow_execution_history(user_id);
CREATE INDEX idx_execution_started_at ON workflow_execution_history(started_at);
