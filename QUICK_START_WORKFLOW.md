# 工作流功能快速启动指南

## 🚀 5分钟快速体验

### 步骤1: 初始化数据库 (1分钟)

```bash
# 进入项目根目录
cd F:\code\mycode\agent-mesh

# 执行主SQL脚本 (已包含工作流表)
psql -U postgres -d agent_mesh -f sql/agent_mesh.sql
```

**注意**: `agent_mesh.sql` 已经包含了工作流相关的表定义，无需单独执行 `workflow_tables.sql`。

如果使用的是其他数据库客户端（如 pgAdmin、DBeaver），可以直接打开 `sql/agent_mesh.sql` 文件并执行全部内容。

### 步骤2: 启动后端服务 (2分钟)

```bash
# Maven编译并启动
mvn clean spring-boot:run

# 或者在IDEA中直接运行 AgentMeshApplication.java
```

等待看到以下日志表示启动成功:
```
Started AgentMeshApplication in X.XXX seconds
```

### 步骤3: 测试工作流API (2分钟)

#### 方式一: 浏览器访问Swagger文档
打开浏览器访问: `http://localhost:8080/doc.html`

找到 **"工作流管理"** 分组,点击 **"执行订单退款工作流"**,填写参数:
- orderId: `ORDER123`
- userId: `1`
- refundReason: `商品质量问题`

点击 **"发送"** 查看执行结果。

#### 方式二: 使用curl命令
```bash
# PowerShell
curl -X POST "http://localhost:8080/api/workflow/order-refund?orderId=ORDER123&userId=1&refundReason=商品质量问题"

# 或 CMD
curl -X POST "http://localhost:8080/api/workflow/order-refund?orderId=ORDER123^&userId=1^&refundReason=商品质量问题"
```

#### 方式三: 使用Postman/Apifox
1. 新建POST请求: `http://localhost:8080/api/workflow/order-refund`
2. Params标签页添加:
   - orderId: `ORDER123`
   - userId: `1`
   - refundReason: `商品质量问题`
3. 点击Send

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "executionId": "wf_1_1713513600000",
    "workflowId": null,
    "success": true,
    "output": null,
    "executionPath": ["start", "query_order", "check_refund_eligible", ...],
    "nodeResults": {
      "query_order": {
        "nodeId": "query_order",
        "nodeName": "查询订单",
        "success": true,
        "durationMs": 1234
      }
    },
    "totalDurationMs": 5678,
    "errorMessage": null,
    "finalVariables": {...}
  }
}
```

---

## 🔍 查看工作流定义

### 获取订单处理工作流结构
```bash
curl http://localhost:8080/api/workflow/order-processing/definition
```

这会返回完整的工作流JSON定义,包含所有节点配置。

### 获取并行查询工作流结构
```bash
curl http://localhost:8080/api/workflow/parallel-query/definition
```

---

## 🐛 常见问题排查

### 问题1: 数据库表不存在
**错误信息**: `relation "workflow_definition" does not exist`

**解决方案**:
```bash
# 确认已执行SQL脚本
psql -U postgres -d agent_mesh -c "\dt workflow*"

# 如果表不存在,重新执行
psql -U postgres -d agent_mesh -f sql/workflow_tables.sql
```

### 问题2: 工具/智能体ID不存在
**错误信息**: `工具不存在` 或 `智能体不存在`

**解决方案**:
工作流示例中使用了假设的工具ID(1,2,3,4)和智能体ID(100,101)。你需要:

1. **选项A**: 修改工作流定义中的resourceId为实际存在的ID
```java
// 在 OrderProcessingWorkflowService.java 中修改
.resourceId(你的实际工具ID)  // 替换1L, 2L, 3L等
```

2. **选项B**: 创建测试用的工具和智能体
```sql
-- 插入测试工具
INSERT INTO tools (name, display_name, source_type, ...) VALUES (...);

-- 插入测试智能体
INSERT INTO agent (name, user_id, system_prompt, ...) VALUES (...);
```

### 问题3: 端口被占用
**错误信息**: `Port 8080 was already in use`

**解决方案**:
```yaml
# 修改 application.yaml
server:
  port: 8081  # 改为其他端口
```

然后访问: `http://localhost:8081/doc.html`

### 问题4: 编译错误
**错误信息**: 找不到符号 `WorkflowDefinition`

**解决方案**:
```bash
# 清理并重新编译
mvn clean compile

# 或在IDEA中: Build → Rebuild Project
```

---

## 📖 深入学习

### 1. 阅读核心代码
- 工作流引擎实现: `WorkflowEngineImpl.java` (427行)
- 订单处理示例: `OrderProcessingWorkflowService.java` (304行)

### 2. 查看完整文档
- [WORKFLOW_ENHANCEMENT.md](WORKFLOW_ENHANCEMENT.md) - 增强方案说明
- [WORKFLOW_USAGE_GUIDE.md](WORKFLOW_USAGE_GUIDE.md) - 详细使用指南
- [毕设完善总结.md](毕设完善总结.md) - 毕业论文写作指导

### 3. 自定义工作流
参考 `OrderProcessingWorkflowService.java`,创建你自己的工作流:

```java
public WorkflowDefinition createMyWorkflow() {
    List<WorkflowNode> nodes = new ArrayList<>();
    
    // 添加你的节点
    nodes.add(WorkflowNode.builder()
        .nodeId("step1")
        .nodeName("我的步骤1")
        .nodeType(WorkflowNode.NodeType.TOOL_CALL)
        .resourceId(yourToolId)
        .build());
    
    // ... 更多节点
    
    return WorkflowDefinition.builder()
        .workflowName("我的工作流")
        .nodes(nodes)
        .startNodeId("start")
        .build();
}
```

---

## 🎯 下一步行动

### ✅ 立即可以做的
1. 测试订单退款工作流API
2. 查看工作流定义的JSON结构
3. 阅读`WorkflowEngineImpl.java`理解核心逻辑

### 📅 短期计划 (1-2天)
1. 创建实际的测试工具和智能体
2. 修改工作流示例使用真实ID
3. 编写单元测试验证工作流执行

### 📅 中期计划 (1周)
1. 将工作流引擎集成到DecisionExecutor
2. 实现SSE流式输出工作流进度
3. 前端添加工作流可视化展示

### 📅 长期计划 (1月)
1. 开发工作流可视化编辑器
2. 实现工作流版本管理
3. 添加工作流模板市场

---

## 💡 提示

### 调试技巧
```java
// 在 WorkflowEngineImpl.java 中添加日志
log.info("当前节点: {}, 类型: {}", node.getNodeId(), node.getNodeType());
log.info("上下文变量: {}", context.keySet());
log.info("执行结果: {}", result);
```

### 性能优化
```java
// 调整线程池大小
private final ExecutorService workflowExecutor = 
    Executors.newFixedThreadPool(20);  // 默认10,可根据服务器配置调整
```

### 监控建议
```sql
-- 查询工作流执行统计
SELECT 
    workflow_id,
    COUNT(*) as total_executions,
    AVG(total_duration_ms) as avg_duration,
    COUNT(*) FILTER (WHERE success = false) as failed_count
FROM workflow_execution_history
GROUP BY workflow_id;
```

---

## 📞 需要帮助?

如果遇到问题:
1. 查看日志文件: `logs/agent-mesh.log`
2. 检查数据库连接配置: `application-dev.yaml`
3. 参考详细文档: `WORKFLOW_USAGE_GUIDE.md`
4. 查看示例代码: `OrderProcessingWorkflowService.java`

---

**祝你使用愉快!** 🎉

如有任何问题,欢迎查阅完整文档或联系作者。

**作者**: abcLiyew  
**GitHub**: https://github.com/abcLiyew
