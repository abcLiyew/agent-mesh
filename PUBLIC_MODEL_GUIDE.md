# 公共模型配置指南

## 概述

本系统支持**公共模型**功能：`user_id = 1` 的用户创建的模型可以被所有用户使用。

## 实现原理

通过修改数据库触发器 `check_agent_model_ownership()`，在验证模型所有权时，允许以下两种情况：
1. 模型属于当前用户（`user_id = 当前用户ID`）
2. 模型是公共模型（`user_id = 1`）

## 配置步骤

### 1. 执行数据库更新脚本

```bash
psql -U agent_mesh -d agent_mesh -f sql/update_public_model_trigger.sql
```

或在数据库管理工具中执行 `sql/update_public_model_trigger.sql` 文件的内容。

### 2. 确保 user_id = 1 的用户存在

检查数据库中是否有 `id = 1` 的用户：

```sql
SELECT id, username, user_role FROM "user" WHERE id = 1;
```

如果不存在，需要创建一个管理员用户：

```sql
INSERT INTO "user" (id, username, password_hash, email, user_role, is_delete)
VALUES (1, 'admin', '$2a$10$...', 'admin@example.com', 99, 0);
```

### 3. 为 user_id = 1 创建公共模型

使用管理员账号（user_id = 1）登录系统，创建常用的公共模型，例如：

#### 示例：创建 Ollama 公共模型

```sql
-- 首先创建模型提供商（如果不存在）
INSERT INTO model_provider (
    id, user_id, provider_name, provider_code, base_url, 
    api_key_encrypted, status, is_public, is_delete
) VALUES (
    1, 1, '公共 Ollama', 'ollama', 'http://localhost:11434',
    'not-needed', 1, true, 0
);

-- 然后创建公共模型
INSERT INTO ai_model (
    user_id, provider_id, model_name, model_display_name, 
    model_type, context_window, max_tokens, 
    input_cost_per_1k, output_cost_per_1k, 
    currency_type, is_active, is_delete
) VALUES 
(1, 1, 'qwen-plus', '通义千问 Plus (公共)', 'CHAT', 32000, 8000, 
 0.004, 0.012, 'CNY', true, 0),
(1, 1, 'gemma:2b', 'Gemma 2B 快速决策 (公共)', 'CHAT', 8000, 2000, 
 0.001, 0.001, 'CNY', true, 0),
(1, 1, 'text-embedding-v2', '文本嵌入 V2 (公共)', 'EMBEDDING', 512, 512, 
 0.0007, 0, 'CNY', true, 0);
```

### 4. 验证配置

测试普通用户是否可以使用公共模型创建智能体：

```sql
-- 查询公共模型
SELECT id, model_name, model_display_name, user_id 
FROM ai_model 
WHERE user_id = 1 AND is_delete = 0 AND is_active = true;

-- 尝试用普通用户创建智能体（使用公共模型ID）
INSERT INTO agent (
    user_id, name, description, system_prompt,
    decision_model_id, response_model_id, status
) VALUES (
    2049199689690959874,  -- 普通用户ID
    '测试智能体',
    '使用公共模型的测试',
    '你是一个助手',
    1,  -- 公共决策模型ID
    2,  -- 公共回复模型ID
    1
);
```

如果成功插入，说明配置正确！

## 前端适配建议

### 1. 获取可用模型列表

前端应该同时获取：
- 用户自己创建的模型
- 公共模型（user_id = 1）

API 调用示例：

```javascript
// 获取用户可用的模型列表
async function getAvailableModels(userId) {
  const response = await fetch(`/ai-models/available?userId=${userId}`);
  return response.json();
}
```

### 2. 后端接口实现

需要在 `AiModelController` 中添加新接口：

```java
@GetMapping("/available")
public BaseResponse<List<AiModel>> getAvailableModels(
    @RequestParam Long userId
) {
    // 查询用户自己的模型 + 公共模型
    List<AiModel> models = aiModelService.getAvailableModels(userId);
    return ResultUtils.success(models);
}
```

在 `AiModelService` 中实现：

```java
public List<AiModel> getAvailableModels(Long userId) {
    return aiModelDao.lambdaQuery()
        .and(wrapper -> wrapper
            .eq(AiModel::getUserId, userId)
            .or()
            .eq(AiModel::getUserId, 1)  // 公共模型
        )
        .eq(AiModel::getIsDelete, 0)
        .eq(AiModel::getIsActive, true)
        .orderByDesc(AiModel::getCreatedAt)
        .list();
}
```

### 3. 前端显示优化

在模型选择下拉框中，标记公共模型：

```vue
<el-select v-model="form.decisionModelId" placeholder="选择决策模型">
  <el-option
    v-for="model in availableModels"
    :key="model.id"
    :label="model.modelDisplayName"
    :value="model.id"
  >
    <span>{{ model.modelDisplayName }}</span>
    <el-tag v-if="model.userId === 1" size="small" type="success">公共</el-tag>
  </el-option>
</el-select>
```

## 管理公共模型

### 查看公共模型

```sql
SELECT 
    m.id,
    m.model_name,
    m.model_display_name,
    m.model_type,
    p.provider_name,
    m.is_active,
    m.created_at
FROM ai_model m
JOIN model_provider p ON m.provider_id = p.id
WHERE m.user_id = 1 AND m.is_delete = 0
ORDER BY m.created_at DESC;
```

### 停用/启用公共模型

```sql
-- 停用模型
UPDATE ai_model SET is_active = false WHERE id = 1 AND user_id = 1;

-- 启用模型
UPDATE ai_model SET is_active = true WHERE id = 1 AND user_id = 1;
```

### 删除公共模型

```sql
-- 逻辑删除
UPDATE ai_model SET is_delete = 1 WHERE id = 1 AND user_id = 1;
```

## 注意事项

### 安全性
1. **仅信任的管理员**可以操作 `user_id = 1` 的模型
2. 公共模型应该是**稳定、可靠**的模型
3. 定期检查公共模型的状态和可用性

### 性能
1. 公共模型会被多个用户使用，注意**并发限制**
2. 监控公共模型的**调用频率**和**成本**
3. 考虑为公共模型设置**配额限制**

### 维护
1. 定期更新公共模型的配置（如 API Key）
2. 监控公共模型的**响应时间**和**错误率**
3. 及时停用不可用的公共模型

## 扩展建议

### 1. 添加模型可见性字段

在 `ai_model` 表中添加 `visibility` 字段：

```sql
ALTER TABLE ai_model ADD COLUMN visibility SMALLINT DEFAULT 0;
-- 0=私有, 1=团队共享, 2=公开
```

然后修改触发器：

```sql
WHERE id = NEW.decision_model_id 
AND (user_id = NEW.user_id OR visibility = 2)  -- 更灵活的权限控制
```

### 2. 添加模型使用统计

记录每个公共模型的使用情况：

```sql
CREATE TABLE public_model_usage (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3. 添加模型评分和反馈

让用户对公共模型进行评分，帮助其他用户选择：

```sql
ALTER TABLE ai_model ADD COLUMN rating_avg NUMERIC(3,2) DEFAULT 0;
ALTER TABLE ai_model ADD COLUMN rating_count INTEGER DEFAULT 0;
```

## 故障排查

### 问题1：仍然提示模型不属于用户

**检查清单**：
- [ ] 确认已执行 `update_public_model_trigger.sql`
- [ ] 确认模型的 `user_id = 1`
- [ ] 确认模型 `is_delete = 0` 且 `is_active = true`
- [ ] 重启应用（清除可能的缓存）

**验证触发器是否更新**：

```sql
SELECT prosrc FROM pg_proc WHERE proname = 'check_agent_model_ownership';
```

应该看到包含 `OR user_id = 1` 的代码。

### 问题2：找不到 user_id = 1 的用户

**解决方案**：

```sql
-- 查看所有用户ID
SELECT id, username FROM "user" ORDER BY id;

-- 如果需要使用其他ID作为公共模型所有者，修改触发器中的数字
-- 将 user_id = 1 改为实际的ID
```

### 问题3：公共模型调用失败

**可能原因**：
- 模型的 API Key 过期或无效
- 模型服务不可用
- 配额用尽

**解决方案**：
1. 检查模型提供商配置
2. 测试模型连接
3. 查看日志中的详细错误信息

## 总结

通过以上配置，你实现了：
✅ 公共模型功能（user_id = 1 的模型所有人可用）
✅ 保持了安全性（仍会验证模型所有权）
✅ 易于管理和维护
✅ 可扩展的设计

现在你可以：
1. 执行 SQL 脚本更新触发器
2. 为 user_id = 1 创建常用公共模型
3. 前端适配显示公共模型
4. 所有用户都可以使用这些公共模型创建智能体！