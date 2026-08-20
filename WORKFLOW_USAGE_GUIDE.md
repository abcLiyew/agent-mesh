# 工作流功能使用指南

## 🚀 快速开始

### 1. 初始化数据库

执行SQL脚本创建工作流相关表:
```bash
psql -U postgres -d agent_mesh -f sql/workflow_tables.sql
```

### 2. 启动后端服务

确保Spring Boot应用正常运行:
```bash
mvn spring-boot:run
```

### 3. 测试订单退款工作流

#### 方式一: 通过Swagger UI
访问 `http://localhost:8080/doc.html` (Knife4j接口文档)
找到"工作流管理"分组,测试以下接口:
- `POST /api/workflow/order-refund`
- `GET /api/workflow/order-processing/definition`

#### 方式二: 使用curl命令
```bash
# 执行订单退款工作流
curl -X POST "http://localhost:8080/api/workflow/order-refund?orderId=ORDER123&userId=1&refundReason=商品质量问题" \
  -H "Content-Type: application/json"

# 获取工作流定义
curl "http://localhost:8080/api/workflow/order-processing/definition"
```

#### 方式三: 编写测试代码
```java
@SpringBootTest
class WorkflowTest {
    
    @Resource
    private OrderProcessingWorkflowService workflowService;
    
    @Test
    void testOrderRefund() {
        WorkflowExecutionResult result = workflowService.processOrderRefund(
            "ORDER123", 
            1L, 
            "用户申请退款"
        );
        
        assertTrue(result.getSuccess());
        assertNotNull(result.getOutput());
        System.out.println("执行路径: " + result.getExecutionPath());
        System.out.println("总耗时: " + result.getTotalDurationMs() + "ms");
    }
}
```

---

## 📖 工作流节点类型详解

### 1. START - 起始节点
```java
WorkflowNode start = WorkflowNode.builder()
    .nodeId("start")
    .nodeName("开始")
    .nodeType(WorkflowNode.NodeType.START)
    .build();
```

### 2. TOOL_CALL - 工具调用节点
```java
WorkflowNode toolNode = WorkflowNode.builder()
    .nodeId("query_order")
    .nodeName("查询订单")
    .nodeType(WorkflowNode.NodeType.TOOL_CALL)
    .resourceId(1L)  // 工具ID
    .resourceType(WorkflowNode.ResourceType.TOOL)
    .inputParams(Map.of(
        "order_id", "${order_id}",  // 从上下文获取参数
        "user_id", "${user_id}"
    ))
    .timeoutMs(10000L)  // 超时10秒
    .retryCount(2)      // 失败重试2次
    .errorStrategy("FAIL_FAST")  // 失败策略
    .build();
```

**参数表达式说明**:
- `${order_id}`: 从上下文中读取变量`order_id`的值
- 支持嵌套访问: `${result.data.id}`
- 支持默认值: `${order_id:DEFAULT123}`

### 3. AGENT_CALL - 智能体调用节点
```java
WorkflowNode agentNode = WorkflowNode.builder()
    .nodeId("generate_response")
    .nodeName("生成回复")
    .nodeType(WorkflowNode.NodeType.AGENT_CALL)
    .resourceId(100L)  // 智能体ID
    .resourceType(WorkflowNode.ResourceType.AGENT)
    .inputParams(Map.of(
        "context", "${query_result}",
        "original_query", "${original_query}"
    ))
    .build();
```

### 4. CONDITION - 条件判断节点
```java
WorkflowNode conditionNode = WorkflowNode.builder()
    .nodeId("check_refund")
    .nodeName("检查退款资格")
    .nodeType(WorkflowNode.NodeType.CONDITION)
    .conditionExpression("${query_result.can_refund}")  // 条件表达式
    .trueBranch("process_refund")   // 条件为真时执行的节点
    .falseBranch("reject_refund")   // 条件为假时执行的节点
    .build();
```

**支持的表达式**:
- 简单布尔值: `"${can_refund}"` → true/false
- 比较运算: `"${amount} > 100"` (需要集成SpEL)
- 字符串匹配: `"${status} == 'success'"`

### 5. SEQUENCE - 顺序执行节点
```java
List<String> steps = Arrays.asList("step1", "step2", "step3");
WorkflowNode sequenceNode = WorkflowNode.builder()
    .nodeId("sequential_steps")
    .nodeName("顺序执行步骤")
    .nodeType(WorkflowNode.NodeType.SEQUENCE)
    .childNodes(steps)  // 子节点列表
    .build();
```

**执行逻辑**: 按顺序依次执行step1 → step2 → step3

### 6. PARALLEL - 并行执行节点
```java
List<String> parallelSteps = Arrays.asList("query_db", "query_cache", "query_api");
WorkflowNode parallelNode = WorkflowNode.builder()
    .nodeId("parallel_queries")
    .nodeName("并行查询")
    .nodeType(WorkflowNode.NodeType.PARALLEL)
    .childNodes(parallelSteps)
    .build();
```

**执行逻辑**: 同时执行三个查询,等待全部完成后继续

### 7. END - 结束节点
```java
WorkflowNode end = WorkflowNode.builder()
    .nodeId("end")
    .nodeName("结束")
    .nodeType(WorkflowNode.NodeType.END)
    .build();
```

---

## 🔧 错误处理策略

### FAIL_FAST - 快速失败
```java
.errorStrategy("FAIL_FAST")
```
- 节点执行失败立即终止整个工作流
- 适用场景:关键步骤失败无需继续

### CONTINUE - 继续执行
```java
.errorStrategy("CONTINUE")
```
- 节点失败后记录错误,继续执行后续节点
- 适用场景:非关键步骤,允许部分失败

### RETRY - 重试机制
```java
.errorStrategy("RETRY")
.retryCount(3)  // 最多重试3次
```
- 节点失败后自动重试指定次数
- 适用场景:网络请求、第三方API调用等临时性故障

---

## 💡 最佳实践

### 1. 工作流设计原则
- ✅ **单一职责**:每个工作流只负责一个业务流程
- ✅ **节点粒度适中**:不要过于细化或粗化
- ✅ **明确错误处理**:为关键节点配置合适的错误策略
- ✅ **设置合理超时**:避免长时间阻塞

### 2. 参数传递技巧
```java
// 好的做法:使用有意义的变量名
.inputParams(Map.of(
    "order_id", "${order_id}",
    "user_id", "${user_id}"
))

// 避免:硬编码值
.inputParams(Map.of(
    "order_id", "ORDER123"  // ❌ 不灵活
))
```

### 3. 性能优化
```java
// 并行执行独立任务
WorkflowNode parallel = WorkflowNode.builder()
    .nodeType(WorkflowNode.NodeType.PARALLEL)
    .childNodes(Arrays.asList("query_user", "query_orders", "query_prefs"))
    .build();

// 而非串行执行
// query_user → query_orders → query_prefs (慢3倍)
```

### 4. 调试技巧
```java
// 查看执行路径
System.out.println("执行路径: " + result.getExecutionPath());

// 查看每个节点的执行结果
result.getNodeResults().forEach((nodeId, nodeResult) -> {
    System.out.println(nodeId + ": " + 
        (nodeResult.getSuccess() ? "成功" : "失败: " + nodeResult.getErrorMessage()));
});

// 查看总耗时
System.out.println("总耗时: " + result.getTotalDurationMs() + "ms");
```

---

## 🐛 常见问题

### Q1: 节点执行失败怎么办?
**A**: 检查以下几点:
1. 工具/智能体ID是否正确
2. 输入参数是否符合要求
3. 错误策略是否配置合理
4. 查看`nodeResults`中的详细错误信息

### Q2: 如何在节点间传递数据?
**A**: 使用表达式`${nodeId_result}`:
```java
// 节点1的输出会自动保存为 query_order_result
// 节点2可以通过表达式引用
.inputParams(Map.of("order_info", "${query_order_result}"))
```

### Q3: 并行节点如何合并结果?
**A**: 所有并行节点的结果都会保存到上下文,后续节点可以访问:
```java
// 并行执行后,上下文包含:
// - query_user_result
// - query_orders_result  
// - query_prefs_result

// 后续节点可以聚合这些数据
.inputParams(Map.of(
    "user_data", "${query_user_result}",
    "order_data", "${query_orders_result}",
    "pref_data", "${query_prefs_result}"
))
```

### Q4: 工作流执行超时如何处理?
**A**: 设置合理的超时时间:
```java
// 节点级超时
.timeoutMs(5000L)  // 5秒

// 全局超时
WorkflowDefinition.builder()
    .timeoutMs(60000L)  // 60秒
    .build();
```

---

## 📊 监控与日志

### 日志级别
```properties
# application.yaml
logging:
  level:
    com.esdllm.agentmesh.service.workflow: DEBUG
```

### 关键日志
```
INFO  - 开始执行工作流, executionId: wf_1_1234567890, workflowId: 1
INFO  - 执行节点: 查询订单, 类型: TOOL_CALL
INFO  - 调用工具: 订单查询工具, 参数: {order_id=ORDER123}
INFO  - 节点执行成功: query_order, duration: 1234ms
INFO  - 工作流执行成功, executionId: wf_1_1234567890, duration: 5678ms
```

### 性能监控
```sql
-- 查询平均执行时间
SELECT 
    workflow_id,
    AVG(total_duration_ms) as avg_duration,
    MAX(total_duration_ms) as max_duration,
    COUNT(*) as execution_count
FROM workflow_execution_history
WHERE success = true
GROUP BY workflow_id;

-- 查询失败率
SELECT 
    workflow_id,
    COUNT(*) FILTER (WHERE success = false) * 100.0 / COUNT(*) as failure_rate
FROM workflow_execution_history
GROUP BY workflow_id;
```

---

## 🎯 扩展开发

### 添加自定义节点类型
```java
// 1. 在WorkflowNode.NodeType枚举中添加新类型
public enum NodeType {
    // ... existing types ...
    CUSTOM_ACTION  // 自定义动作
}

// 2. 在WorkflowEngineImpl.executeNode()中添加处理逻辑
case CUSTOM_ACTION:
    output = executeCustomAction(node, context, userId);
    break;

// 3. 实现自定义逻辑
private Object executeCustomAction(WorkflowNode node, Map<String, Object> context, Long userId) {
    // 你的自定义逻辑
    return result;
}
```

### 集成外部工作流引擎
如果需要更复杂的工作流功能,可以集成:
- Camunda BPMN
- Apache Airflow
- Netflix Conductor

---

**最后更新**: 2026-04-19  
**维护者**: abcLiyew
