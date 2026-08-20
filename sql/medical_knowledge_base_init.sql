-- 医疗领域知识库初始化脚本
-- 创建一个医疗知识库及示例文档

BEGIN;

-- 1. 创建医疗知识库
INSERT INTO knowledge_base (
    user_id, 
    name, 
    description, 
    vector_store_type, 
    vector_store_table, 
    embedding_model_id, 
    chunk_size, 
    chunk_overlap, 
    status, 
    is_delete, 
    visibility
) VALUES (
    1, -- 用户ID，根据实际情况调整
    '医疗健康知识库',
    '包含常见疾病、症状、治疗方案等医疗信息的知识库，用于辅助医疗问答和健康咨询',
    'OLLAMA',
    'ollama_vector_store',
    3, -- 嵌入模型ID，需要根据实际模型调整
    500,
    50,
    1, -- 状态：启用
    0, -- 未删除
    2  -- 可见性：公开
);

-- 获取刚插入的知识库ID（在实际使用中需要替换为实际的ID）
-- 假设知识库ID为1，下面插入示例文档

-- 2. 插入示例文档 - 常见疾病信息
INSERT INTO knowledge_base_document (
    kb_id,
    doc_name,
    doc_type,
    source_url,
    content_hash,
    chunk_count,
    vector_ids,
    metadata_json,
    status,
    is_delete,
    related_tool_ids
) VALUES (
    1, -- kb_id，需要根据实际情况调整
    '常见疾病诊疗指南',
    'TEXT',
    NULL,
    'hash_common_diseases_001',
    1,
    '[]'::jsonb,
    '{"author": "医疗专家组", "category": "疾病诊疗", "version": "1.0"}'::jsonb,
    1, -- 处理完成
    0, -- 未删除
    '[]'::jsonb
);

-- 3. 插入示例文档 - 药物信息
INSERT INTO knowledge_base_document (
    kb_id,
    doc_name,
    doc_type,
    source_url,
    content_hash,
    chunk_count,
    vector_ids,
    metadata_json,
    status,
    is_delete,
    related_tool_ids
) VALUES (
    1, -- kb_id，需要根据实际情况调整
    '常用药物说明书',
    'TEXT',
    NULL,
    'hash_medications_001',
    1,
    '[]'::jsonb,
    '{"author": "药学部", "category": "药物信息", "version": "1.0"}'::jsonb,
    1, -- 处理完成
    0, -- 未删除
    '[]'::jsonb
);

-- 4. 插入示例文档 - 健康生活方式
INSERT INTO knowledge_base_document (
    kb_id,
    doc_name,
    doc_type,
    source_url,
    content_hash,
    chunk_count,
    vector_ids,
    metadata_json,
    status,
    is_delete,
    related_tool_ids
) VALUES (
    1, -- kb_id，需要根据实际情况调整
    '健康生活指南',
    'TEXT',
    NULL,
    'hash_health_lifestyle_001',
    1,
    '[]'::jsonb,
    '{"author": "健康管理师", "category": "健康生活方式", "version": "1.0"}'::jsonb,
    1, -- 处理完成
    0, -- 未删除
    '[]'::jsonb
);

COMMIT;

-- 注意：在实际应用中，你需要：
-- 1. 确保embedding_model_id指向有效的嵌入模型
-- 2. 对文档内容进行向量化处理并更新vector_ids字段
-- 3. 根据实际需求调整chunk_size和chunk_overlap参数