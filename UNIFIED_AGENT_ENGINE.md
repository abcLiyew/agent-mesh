# 统一智能体工作流引擎（龙虾架构）

## 📖 概述

本项目已成功将**智能体决策引擎**和**工作流引擎**融合，并参考"龙虾"(OpenClaw)架构实现了以下核心能力：

### 🦞 龙虾架构核心特性

1. **主智能体决策引擎** - 负责任务拆解、意图识别和调度决策
2. **子智能体执行模块** - 具体执行单元，支持工具和智能体调用
3. **自适应规则工作流** - 灵动、自驱、自适应的流程编排
4. **长期记忆系统** - 跨会话、跨周期的记忆能力
5. **技能市场机制** - 可插拔的能力包动态加载

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────┐
│           Unified Agent Engine (统一引擎)             │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────────────────┐   │
│  │ 决策引擎      │    │ 工作流引擎                │   │
│  │ Decision     │◄──►│ Workflow                 │   │
│  │ Executor     │    │ Engine                   │   │
│  └──────────────┘    └──────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────────────────┐   │
│  │ 长期记忆      │    │ 技能市场                  │   │
│  │ Long-Term    │    │ Skill Market             │   │
│  │ Memory       │    │                          │   │
│  └──────────────┘    └──────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│              感知-决策-执行-反馈闭环                   │
└─────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 数据库初始化

执行SQL脚本创建新表：

```bash
psql -U postgres -d agent_mesh -f sql/agent_mesh.sql
```

新增的表包括：
- `agent_long_term_memory` - 长期记忆表
- `agent_skill_package` - 技能包表
- `user_skill_installation` - 用户技能安装记录表

### 2. 启动服务

```bash
mvn clean spring-boot:run
```

### 3. API测试

#### 方式一：同步执行（自主决策）

```bash
curl -X POST "http://localhost:8080/api/unified-agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": 1,
    "query": "帮我查询订单ORDER123的状态",
    "context": {
      "sessionId": "session_001"
    }
  }'
```

#### 方式二：指定工作流执行

```bash
curl -X POST "http://localhost:8080/api/unified-agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": 1,
    "query": "处理订单退款",
    "workflowId": 1,
    "context": {
      "orderId": "ORDER123",
      "refundReason": "商品质量问题"
    }
  }'
```

#### 方式三：流式执行（SSE）

```bash
curl -N "http://localhost:8080/api/unified-agent/execute-stream?agentId=1&query=查询订单状态"
```

## 📚 核心功能说明

### 1. 长期记忆系统

**功能特点：**
- 跨会话记忆：记住用户偏好、项目背景
- 智能检索：基于类型、重要性、访问频率排序
- 自动提取：从对话中自动提取关键信息
- 定期清理：自动清理过期记忆

**API示例：**

```java
@Resource
private LongTermMemoryService memoryService;

// 存储记忆
AgentLongTermMemory memory = new AgentLongTermMemory();
memory.setUserId(userId);
memory.setMemoryType("USER_PREFERENCE");
memory.setContent("用户喜欢简洁的回答风格");
memory.setImportance(7);
Long memoryId = memoryService.storeMemory(memory);

// 检索记忆
List<AgentLongTermMemory> memories = memoryService.retrieveMemories(
    userId, agentId, "回答风格", 
    Arrays.asList("USER_PREFERENCE"), 5
);

// 获取用户画像
Map<String, Object> profile = memoryService.getUserProfile(userId);
```

### 2. 技能市场

**功能特点：**
- 技能发布：开发者可发布 reusable 技能包
- 一键安装：用户无需编程即可集成新能力
- 版本管理：支持多版本共存和升级
- 评分系统：社区评价和推荐

**API示例：**

```java
@Resource
private SkillMarketService skillMarketService;

// 发布技能
AgentSkillPackage skill = new AgentSkillPackage();
skill.setSkillName("PDF解析器");
skill.setCategory("DOCUMENT_ANALYSIS");
skill.setIsPublic(true);
Long skillId = skillMarketService.publishSkill(skill);

// 安装技能
Long installationId = skillMarketService.installSkill(
    userId, agentId, skillId, 
    Map.of("maxFileSize", "10MB")
);

// 执行技能
Object result = skillMarketService.executeSkill(
    installationId, 
    Map.of("filePath", "/path/to/file.pdf"),
    userId
);
```

### 3. 统一执行引擎

**执行模式：**

1. **自主决策模式**（不传workflowId）
   - AI自动识别意图
   - 匹配工具和知识库
   - 生成执行路径

2. **工作流模式**（传入workflowId）
   - 按照预定义流程执行
   - 支持条件分支、并行节点
   - 可调用多个智能体和工具

3. **混合模式**（开发中）
   - AI动态生成工作流
   - 结合历史经验和当前上下文
   - 自适应调整执行策略

## 🔧 技术实现细节

### 核心类说明

| 类名 | 职责 | 路径 |
|------|------|------|
| `UnifiedAgentEngine` | 统一引擎接口 | `service/unified/` |
| `UnifiedAgentEngineImpl` | 统一引擎实现 | `service/unified/impl/` |
| `LongTermMemoryService` | 长期记忆服务接口 | `service/unified/` |
| `LongTermMemoryServiceImpl` | 长期记忆服务实现 | `service/unified/impl/` |
| `SkillMarketService` | 技能市场服务接口 | `service/unified/` |
| `UnifiedAgentController` | 统一API控制器 | `controller/` |

### 数据模型

| 实体 | 说明 | 关键字段 |
|------|------|----------|
| `AgentLongTermMemory` | 长期记忆 | userId, memoryType, content, importance |
| `AgentSkillPackage` | 技能包 | skillName, category, skillDefinitionJson |
| `UserSkillInstallation` | 技能安装 | userId, skillPackageId, configJson |

## 📊 与原有系统的关系

### 兼容性

- ✅ **保留原有决策引擎**：`DecisionExecutor` 继续工作
- ✅ **保留原有工作流引擎**：`WorkflowEngine` 独立可用
- ✅ **向后兼容**：现有API不受影响
- ✅ **渐进式迁移**：可选择性使用统一引擎

### 增强点

| 维度 | 原系统 | 统一引擎 |
|------|--------|----------|
| 记忆能力 | 仅会话级日志 | 跨周期长期记忆 |
| 执行模式 | 单一决策或工作流 | 自主/指定/混合 |
| 扩展性 | 硬编码工具 | 技能市场动态加载 |
| 学习能力 | 无 | 基于反馈优化 |

## 🎯 典型应用场景

### 场景1：智能客服（带记忆）

```
用户A: "我喜欢红色的商品"
→ 系统存储偏好记忆

用户A（第二天）: "给我推荐一些商品"
→ 系统检索记忆，优先推荐红色商品
```

### 场景2：复杂业务流程

```
用户: "处理订单ORDER123的退款"
→ 统一引擎判断为复杂任务
→ 调用订单退款工作流
→ 执行：查询订单 → 验证条件 → 库存回滚 → 支付退款 → 发送通知
→ 记录执行经验到记忆
```

### 场景3：技能扩展

```
开发者发布"Excel数据分析"技能包
→ 用户一键安装到智能体
→ 用户: "分析这个销售数据.xlsx"
→ 智能体调用新技能完成分析
```

## 🔮 未来规划

### 短期（1-2周）

- [ ] 完善工作流执行逻辑（目前为占位实现）
- [ ] 实现AI动态生成工作流功能
- [ ] 添加技能执行的沙箱环境
- [ ] 完善向量相似度记忆检索

### 中期（1个月）

- [ ] 实现多智能体协同机制
- [ ] 添加工作流可视化编辑器
- [ ] 建立技能包审核和推荐系统
- [ ] 优化记忆提取算法（使用LLM）

### 长期（3个月+）

- [ ] 实现自适应规则学习
- [ ] 构建技能生态社区
- [ ] 支持跨组织技能共享
- [ ] 引入强化学习优化决策

## 📝 注意事项

1. **性能考虑**：长期记忆检索可能增加100-300ms延迟，建议设置合理的limit
2. **隐私保护**：敏感信息不应存入长期记忆，需配置脱敏规则
3. **记忆质量**：初期记忆提取较简单，后续会引入AI提升准确性
4. **技能安全**：安装第三方技能包时需审查代码，避免安全风险

## 🤝 贡献指南

欢迎提交Issue和Pull Request来完善统一引擎功能！

---

**最后更新**: 2026-04-19  
**版本**: v1.0.0 (龙虾架构基础版)
