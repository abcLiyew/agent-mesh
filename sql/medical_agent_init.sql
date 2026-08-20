-- 医疗助手智能体初始化脚本
-- 创建一个基于医疗知识库的智能体

BEGIN;

-- 1. 创建医疗助手智能体
INSERT INTO agent (
    user_id,
    name,
    description,
    avatar_url,
    system_prompt,
    role_definition,
    decision_model_id,
    response_model_id,
    is_tool_enabled,
    tool_schema_json,
    tool_description,
    version,
    status,
    is_delete,
    visibility,
    model_selection_strategy,
    budget_constraint
) VALUES (
    1, -- 用户ID，根据实际情况调整
    '医疗健康助手',
    '专业的医疗健康咨询助手，能够回答常见疾病、症状、治疗方案等问题',
    NULL,
    '你是一个专业的医疗健康助手，具备丰富的医学知识。你的主要职责是：
1. 回答用户关于常见疾病的疑问
2. 提供健康生活方式建议
3. 解释医学术语和检查结果
4. 给出一般性的健康指导

重要原则：
- 始终强调你提供的信息仅供参考，不能替代专业医生的诊断和治疗
- 对于紧急或严重情况，建议用户立即就医
- 使用通俗易懂的语言解释医学概念
- 保持专业、准确、负责任的态度
- 不提供具体的药物剂量建议，只说明一般用途
- 尊重用户隐私，不询问敏感个人信息',
    '医疗健康领域的专业顾问，具有内科、全科医学背景，擅长健康咨询和疾病预防指导',
    1, -- 决策模型ID，需要根据实际模型调整
    2, -- 回复模型ID，需要根据实际模型调整
    false, -- 是否启用工具
    NULL,
    NULL,
    '1.0.0',
    1, -- 状态：发布
    0, -- 未删除
    2, -- 可见性：公开
    'ADAPTIVE',
    0.1000 -- 预算约束
);

-- 获取刚插入的智能体ID（在实际使用中需要替换为实际的ID）
-- 假设智能体ID为1，下面建立与知识库的关联

-- 2. 建立智能体与医疗知识库的关联
INSERT INTO agent_kb_relation (
    agent_id,
    kb_id,
    search_top_k,
    similarity_threshold,
    sort_order,
    is_delete
) VALUES (
    1, -- agent_id，需要根据实际情况调整
    1, -- kb_id，需要根据实际情况调整
    5, -- 检索返回的最大结果数
    0.7, -- 相似度阈值
    1, -- 排序顺序
    0 -- 未删除
);

COMMIT;

-- 注意：在实际应用中，你需要：
-- 1. 确保decision_model_id和response_model_id指向有效的模型
-- 2. 根据实际需求调整search_top_k和similarity_threshold参数
-- 3. 可以为智能体添加更多配置，如工具启用等