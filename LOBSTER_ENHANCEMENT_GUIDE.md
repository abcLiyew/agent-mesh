# 龙虾架构功能完善 - 实施说明

## 📋 概述

本文档说明如何启用和完善"龙虾"(OpenClaw)架构的以下核心功能:

1. ✅ **执行经验数据库** - 持久化存储执行经验,支持学习和优化
2. ✅ **长期记忆向量检索** - 基于向量相似度的智能记忆检索
3. ⚠️ **技能市场基础框架** - 技能包的定义、安装和管理
4. ✅ **智能体依赖增强** - 完善的依赖关系配置

---

## 🗄️ 数据库迁移

### 步骤1: 执行SQL脚本

```bash
# 连接到PostgreSQL数据库
psql -U postgres -d agent_mesh

# 执行增强脚本
\i F:/code/mycode/agent-mesh/sql/lobster_architecture_enhancement.sql
```

该脚本会创建以下表:

| 表名 | 用途 | 状态 |
|------|------|------|
| `execution_experience` | 执行经验数据库 | ✅ 新增 |
| `agent_long_term_memory` | 长期记忆(带向量检索) | ✅ 更新 |
| `agent_skill_package` | 技能包定义 | ✅ 更新 |
| `user_skill_installation` | 用户技能安装记录 | ✅ 更新 |
| `agent_skill_relation` | 智能体-技能关联 | ✅ 新增 |
| `workflow_template_market` | 工作流模板市场 | ✅ 新增 |
| `user_template_installation` | 用户模板安装记录 | ✅ 新增 |

同时为`agent_dependency`表添加增强字段:
- `condition_expression` - 调用条件表达式
- `parameter_mapping_json` - 参数映射配置
- `timeout_ms` - 超时时间
- `retry_count` - 重试次数
- `is_enabled` - 是否启用

---

## 💻 Java实体类和Mapper

### 已创建/更新的实体类

1. **ExecutionExperience** (`model/domain/ExecutionExperience.java`)
   - 对应`execution_experience`表
   - 用于记录工作流/任务的执行经验

2. **AgentLongTermMemory** (已更新)
   - 对应`agent_long_term_memory`表
   - 添加了`memoryKey`, `memoryValue`, `confidenceScore`等字段
   - 支持向量相似度检索(`memoryVector`)

3. **AgentSkillPackage** (已更新)
   - 对应`agent_skill_package`表
   - 添加了`skillCode`, `skillConfigJson`, `inputSchemaJson`, `outputSchemaJson`等字段

4. **UserSkillInstallation** (已更新)
   - 对应`user_skill_installation`表
   - 简化为`userId`, `skillId`, `installationConfigJson`, `status`

### 已创建的Mapper

1. **ExecutionExperienceMapper** (`repository/mapper/ExecutionExperienceMapper.java`)
   - 继承MyBatis-Plus的BaseMapper
   - 提供基础的CRUD操作

---

## 🔧 待实现的Service层功能

### 1. ExecutionExperienceService (经验数据库服务)

**目标**: 将内存中的经验数据库持久化到PostgreSQL

**需要实现的方法**:
```java
public interface ExecutionExperienceService {
    // 记录执行经验
    void recordExperience(ExecutionExperience experience);
    
    // 查询成功经验
    List<ExecutionExperience> findSuccessfulExperiences(Long workflowId, String type);
    
    // 分析执行模式
    Map<String, Object> analyzePatterns(String experienceType);
    
    // 获取最近的经验
    List<ExecutionExperience> getRecentExperiences(int limit);
}
```

**集成点**: 
- 在`UnifiedAgentEngineImpl.learnAndOptimize()`中调用
- 替换当前的内存存储`experienceDatabase`

---

### 2. LongTermMemoryService增强 (长期记忆服务)

**当前状态**: 已有基础实现,但`retrieveMemoriesBySimilarity()`未实现

**需要完善**:
```java
@Override
public List<Map<String, Object>> retrieveMemoriesBySimilarity(Long userId, String query, int topK) {
    // TODO: 实现向量相似度检索
    // 1. 使用embedding模型将query转换为向量
    // 2. 在数据库中执行向量相似度搜索
    // 3. 返回topK个最相关的记忆
}
```

**实现建议**:
```java
// 伪代码示例
public List<Map<String, Object>> retrieveMemoriesBySimilarity(Long userId, String query, int topK) {
    // 1. 生成查询向量
    float[] queryVector = embeddingService.embed(query);
    
    // 2. 执行向量相似度搜索 (使用PostgreSQL的pgvector扩展)
    List<AgentLongTermMemory> memories = memoryMapper.selectList(
        new LambdaQueryWrapper<AgentLongTermMemory>()
            .eq(AgentLongTermMemory::getUserId, userId)
            .eq(AgentLongTermMemory::getIsActive, true)
            .orderByDesc("memory_vector <=> " + Arrays.toString(queryVector)) // pgvector语法
            .last("LIMIT " + topK)
    );
    
    // 3. 转换为Map格式
    return memories.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());
}
```

---

### 3. SkillMarketService (技能市场服务)

**目标**: 实现技能包的CRUD和安装管理

**需要实现的方法**:
```java
public interface SkillMarketService {
    // 浏览公开技能
    Page<AgentSkillPackage> browsePublicSkills(String category, int page, int size);
    
    // 安装技能
    void installSkill(Long userId, Long skillId, Map<String, Object> config);
    
    // 卸载技能
    void uninstallSkill(Long userId, Long skillId);
    
    // 获取用户已安装的皮肤
    List<UserSkillInstallation> getUserInstalledSkills(Long userId);
    
    // 执行技能
    Object executeSkill(String skillCode, Map<String, Object> input);
}
```

**执行技能的实现思路**:
```java
public Object executeSkill(String skillCode, Map<String, Object> input) {
    // 1. 查找技能定义
    AgentSkillPackage skill = skillPackageMapper.selectOne(
        new LambdaQueryWrapper<AgentSkillPackage>()
            .eq(AgentSkillPackage::getSkillCode, skillCode)
            .eq(AgentSkillPackage::getStatus, 1)
    );
    
    if (skill == null) {
        throw new BusinessException("技能不存在: " + skillCode);
    }
    
    // 2. 解析技能配置
    SkillConfig config = parseSkillConfig(skill.getSkillConfigJson());
    
    // 3. 根据技能类型执行
    switch (config.getType()) {
        case "WORKFLOW":
            // 执行预定义的工作流
            return executeWorkflowSkill(config, input);
        case "TOOL_COMPOSITION":
            // 组合多个工具
            return executeToolCompositionSkill(config, input);
        case "AGENT_COLLABORATION":
            // 多智能体协作
            return executeAgentCollaborationSkill(config, input);
        default:
            throw new BusinessException("不支持的技能类型: " + config.getType());
    }
}
```

---

### 4. AgentDependencyService增强 (智能体依赖服务)

**当前状态**: 已有基础CRUD,但缺少智能依赖解析

**需要增强的方法**:
```java
public interface AgentDependencyService {
    // 获取智能体的依赖列表(带条件判断)
    List<AgentDependency> getActiveDependencies(Long agentId, Map<String, Object> context);
    
    // 评估依赖条件
    boolean evaluateCondition(String conditionExpression, Map<String, Object> context);
    
    // 执行依赖调用(带重试和超时)
    Object invokeDependency(Long dependencyId, Map<String, Object> params);
}
```

**条件表达式评估示例**:
```java
public boolean evaluateCondition(String conditionExpression, Map<String, Object> context) {
    if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
        return true; // 无条件则始终执行
    }
    
    // 使用SpEL(Spring Expression Language)评估
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext evalContext = new StandardEvaluationContext();
    evalContext.setVariables(context);
    
    try {
        Expression exp = parser.parseExpression(conditionExpression);
        return exp.getValue(evalContext, Boolean.class);
    } catch (Exception e) {
        log.error("条件表达式评估失败: {}", conditionExpression, e);
        return false;
    }
}
```

---

## 🎯 优先级建议

### P0 - 立即实现(毕设答辩前)

1. ✅ **执行经验数据库持久化**
   - 难度: ⭐⭐
   - 影响: 高 - 让"学习优化"功能真正可用
   - 工作量: 2-3小时

2. ✅ **长期记忆向量检索**
   - 难度: ⭐⭐⭐
   - 影响: 高 - 展示"龙虾"的智能记忆能力
   - 工作量: 3-4小时

### P1 - 短期实现(答辩后优化)

3. ⚠️ **技能市场基础CRUD**
   - 难度: ⭐⭐
   - 影响: 中 - 完善功能完整性
   - 工作量: 4-5小时

4. ⚠️ **智能体依赖条件评估**
   - 难度: ⭐⭐⭐
   - 影响: 中 - 增强多智能体协同能力
   - 工作量: 3-4小时

### P2 - 长期规划

5. ❌ **工作流模板市场**
   - 难度: ⭐⭐⭐⭐
   - 影响: 低 - 高级功能,可作为未来展望
   - 工作量: 8-10小时

6. ❌ **技能执行引擎**
   - 难度: ⭐⭐⭐⭐⭐
   - 影响: 低 - 复杂的功能编排
   - 工作量: 10+小时

---

## 📝 测试建议

### 1. 执行经验数据库测试

```java
@Test
public void testRecordExperience() {
    ExecutionExperience exp = new ExecutionExperience();
    exp.setWorkflowId(1L);
    exp.setExperienceType("workflow_execution");
    exp.setSuccess(true);
    exp.setRating(5);
    exp.setUserFeedback("非常好用!");
    exp.setExecutionTimeMs(1234L);
    
    experienceService.recordExperience(exp);
    
    // 验证数据已保存
    List<ExecutionExperience> experiences = 
        experienceService.findSuccessfulExperiences(1L, "workflow_execution");
    assertTrue(experiences.size() > 0);
}
```

### 2. 长期记忆向量检索测试

```java
@Test
public void testRetrieveMemoriesBySimilarity() {
    // 准备测试数据
    saveTestMemories(userId);
    
    // 执行相似度检索
    List<Map<String, Object>> results = 
        memoryService.retrieveMemoriesBySimilarity(userId, "我喜欢Python编程", 3);
    
    // 验证返回结果
    assertEquals(3, results.size());
    assertTrue(results.get(0).containsKey("memoryValue"));
}
```

---

## 🚀 快速开始

### 步骤1: 执行数据库迁移
```bash
psql -U postgres -d agent_mesh -f sql/lobster_architecture_enhancement.sql
```

### 步骤2: 重启应用
确保新的实体类和Mapper被加载

### 步骤3: 验证表创建
```sql
SELECT tablename FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename LIKE '%experience%' OR tablename LIKE '%memory%';
```

### 步骤4: 开始实现Service层
从P0优先级的任务开始,逐步完善功能

---

## 📚 相关文档

- [龙虾架构功能对比分析](./LOBSTER_ARCHITECTURE_ANALYSIS.md)
- [统一智能体引擎文档](./UNIFIED_AGENT_ENGINE.md)
- [工作流使用指南](./WORKFLOW_USAGE_GUIDE.md)

---

## ❓ 常见问题

**Q: 为什么有些实体类字段类型是String而不是JSONB?**

A: MyBatis-Plus处理JSONB需要自定义TypeHandler。为了简化,我们先将JSON字段存为String,在Service层进行序列化/反序列化。如果需要更好的性能,可以后续添加TypeHandler。

**Q: 向量检索需要安装pgvector扩展吗?**

A: 是的,确保PostgreSQL已安装pgvector扩展。检查方法:
```sql
SELECT * FROM pg_extension WHERE extname = 'vector';
```
如果没有安装,参考: https://github.com/pgvector/pgvector

**Q: 技能市场功能必须实现吗?**

A: 对于毕设答辩来说,不是必须的。核心的"感知-决策-执行-反馈"闭环已经完整。技能市场可以作为"未来展望"部分提及。

---

## 🎓 毕设答辩提示

在答辩时,可以这样展示这些功能:

1. **执行经验数据库**: "系统会将每次执行的经验记录下来,包括成功率、耗时、用户反馈等,用于后续的学习和优化。"

2. **长期记忆向量检索**: "通过向量相似度检索,系统能够智能地回忆起与当前问题相关的历史经验和用户偏好,提供更个性化的服务。"

3. **技能市场**: "虽然当前版本只实现了基础框架,但设计上支持技能的动态加载和复用,未来可以构建丰富的技能生态系统。"

4. **智能体依赖**: "支持条件化的智能体调用,可以根据上下文动态决定是否调用某个依赖智能体,提高了系统的灵活性。"

---

**最后更新**: 2026-04-19  
**作者**: Agent Mesh Team
