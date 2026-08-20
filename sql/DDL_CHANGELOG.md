# 数据库DDL变更说明

## 📋 变更概述

已将工作流编排模块的表结构合并到主DDL文件 `sql/agent_mesh.sql` 中。

---

## ✅ 新增表结构

### 1. workflow_definition (工作流定义表)

**用途**: 存储用户定义的复杂业务流程

**关键字段**:
- `id`: 主键
- `workflow_name`: 工作流名称
- `nodes_json`: 节点定义(JSONB格式)
- `start_node_id`: 起始节点ID
- `agent_id`: 关联的智能体ID
- `user_id`: 创建者用户ID
- `enabled`: 是否启用
- `is_delete`: 逻辑删除标记

**索引**:
- `idx_workflow_user_id`: 按用户查询
- `idx_workflow_agent_id`: 按智能体查询
- `idx_workflow_enabled`: 按启用状态查询

### 2. workflow_execution_history (工作流执行历史表)

**用途**: 记录每次工作流执行的详细信息

**关键字段**:
- `execution_id`: 执行ID(唯一)
- `workflow_id`: 工作流ID(外键)
- `user_id`: 用户ID
- `input_params_json`: 输入参数(JSONB)
- `output_result_json`: 输出结果(JSONB)
- `execution_path_json`: 执行路径(JSONB)
- `node_results_json`: 节点执行结果(JSONB)
- `success`: 是否成功
- `total_duration_ms`: 总耗时(毫秒)
- `started_at`: 开始时间
- `completed_at`: 完成时间

**索引**:
- `idx_execution_workflow_id`: 按工作流查询
- `idx_execution_user_id`: 按用户查询
- `idx_execution_started_at`: 按时间查询
- `idx_execution_success`: 按成功状态查询

---

## 🔄 文件变更

### 已修改文件
- ✅ `sql/agent_mesh.sql` - 追加了工作流表定义（+88行）
- ✅ `QUICK_START_WORKFLOW.md` - 更新了初始化说明

### 独立文件（可选）
- `sql/workflow_tables.sql` - 保留作为参考，但不再需要单独执行

---

## 🚀 使用方法

### 方式一: 全新初始化
```bash
# 直接执行主DDL文件（包含所有表）
psql -U postgres -d agent_mesh -f sql/agent_mesh.sql
```

### 方式二: 增量更新
如果数据库已经存在，只需执行新增部分：
```bash
# 从第4436行开始执行（工作流表定义）
psql -U postgres -d agent_mesh <<EOF
-- 复制 agent_mesh.sql 末尾的工作流表定义
EOF
```

### 方式三: GUI工具
使用 pgAdmin、DBeaver 等工具：
1. 打开 `sql/agent_mesh.sql`
2. 执行全部内容
3. 验证表是否创建成功

---

## ✅ 验证步骤

执行以下SQL验证表是否创建成功：

```sql
-- 1. 检查工作流定义表
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'workflow_definition';

-- 2. 检查工作流执行历史表
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'workflow_execution_history';

-- 3. 查看表结构
\d workflow_definition
\d workflow_execution_history

-- 4. 查看索引
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename LIKE 'workflow%';
```

预期输出：
```
table_name
---------------------------
workflow_definition
workflow_execution_history
```

---

## 📊 表关系图

```
┌─────────────────────────────┐
│  workflow_definition        │
│  ─────────────────────────  │
│  id (PK)                    │
│  workflow_name              │
│  nodes_json (JSONB)         │
│  start_node_id              │
│  agent_id → agent(id)       │
│  user_id → user(id)         │
│  enabled                    │
│  is_delete                  │
└─────────────────────────────┘
            │
            │ 1:N
            ▼
┌─────────────────────────────┐
│  workflow_execution_history │
│  ─────────────────────────  │
│  id (PK)                    │
│  execution_id (UNIQUE)      │
│  workflow_id (FK)           │──→ workflow_definition(id)
│  user_id                    │
│  input_params_json (JSONB)  │
│  output_result_json (JSONB) │
│  execution_path_json (JSONB)│
│  node_results_json (JSONB)  │
│  success                    │
│  total_duration_ms          │
│  started_at                 │
│  completed_at               │
└─────────────────────────────┘
```

---

## 🔍 常用查询示例

### 1. 查询用户的工作流列表
```sql
SELECT id, workflow_name, version, enabled, created_at
FROM workflow_definition
WHERE user_id = 1 AND is_delete = 0
ORDER BY created_at DESC;
```

### 2. 查询工作流执行统计
```sql
SELECT 
    wd.workflow_name,
    COUNT(*) as total_executions,
    AVG(weh.total_duration_ms) as avg_duration_ms,
    COUNT(*) FILTER (WHERE weh.success = true) * 100.0 / COUNT(*) as success_rate
FROM workflow_execution_history weh
JOIN workflow_definition wd ON weh.workflow_id = wd.id
GROUP BY wd.workflow_name
ORDER BY total_executions DESC;
```

### 3. 查询失败的工作流执行
```sql
SELECT 
    weh.execution_id,
    wd.workflow_name,
    weh.error_message,
    weh.started_at,
    weh.total_duration_ms
FROM workflow_execution_history weh
JOIN workflow_definition wd ON weh.workflow_id = wd.id
WHERE weh.success = false
ORDER BY weh.started_at DESC
LIMIT 10;
```

### 4. 查询最近的工作流执行
```sql
SELECT 
    execution_id,
    workflow_id,
    success,
    total_duration_ms,
    started_at
FROM workflow_execution_history
WHERE user_id = 1
ORDER BY started_at DESC
LIMIT 20;
```

---

## ⚠️ 注意事项

### 1. 外键约束
`workflow_execution_history.workflow_id` 有外键约束指向 `workflow_definition.id`，删除工作流定义前需要先删除相关的执行历史。

### 2. JSONB字段
所有JSON相关字段使用 `JSONB` 类型，支持高效的JSON查询和索引：
```sql
-- 查询特定节点的执行结果
SELECT * 
FROM workflow_execution_history
WHERE node_results_json->>'query_order' IS NOT NULL;
```

### 3. 权限设置
已为 `agent_mesh` 用户授予必要的权限：
- SELECT, INSERT, UPDATE, DELETE
- USAGE on sequences
- TRIGGER, TRUNCATE, REFERENCES

### 4. 索引优化
已创建6个索引以优化常见查询场景，如需进一步优化可根据实际查询模式添加复合索引。

---

## 📝 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0 | 2026-04-19 | 初始版本，添加工作流表和索引 |

---

## 🔗 相关文档

- [WORKFLOW_ENHANCEMENT.md](WORKFLOW_ENHANCEMENT.md) - 工作流增强方案
- [WORKFLOW_USAGE_GUIDE.md](WORKFLOW_USAGE_GUIDE.md) - 使用指南
- [毕设完善总结.md](毕设完善总结.md) - 毕业论文指导

---

**作者**: abcLiyew  
**学号**: 202252340223  
**日期**: 2026年4月19日
