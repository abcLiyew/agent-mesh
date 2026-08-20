# 工作流编排 API 接口文档

## 📋 概述

本文档描述了工作流编排模块的所有 RESTful API 接口，用于前端开发和集成。

**基础信息**:
- **Base URL**: `http://localhost:8080/api/workflow`
- **Content-Type**: `application/json`
- **认证方式**: 需要在请求头或参数中携带用户ID

---

## 📑 目录

1. [执行订单退款工作流](#1-执行订单退款工作流)
2. [获取订单处理工作流定义](#2-获取订单处理工作流定义)
3. [获取并行查询工作流定义](#3-获取并行查询工作流定义)
4. [执行自定义工作流](#4-执行自定义工作流)

---

## 1. 执行订单退款工作流

### 接口信息
- **路径**: `POST /api/workflow/order-refund`
- **描述**: 执行订单退款自动化流程，包含条件分支、顺序执行等复杂逻辑
- **认证**: 需要 userId

### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| orderId | String | 是 | 订单ID | "ORDER123" |
| userId | Long | 是 | 用户ID | 1 |
| refundReason | String | 否 | 退款原因，默认"用户申请" | "商品质量问题" |

### 请求示例

```bash
curl -X POST "http://localhost:8080/api/workflow/order-refund?orderId=ORDER123&userId=1&refundReason=商品质量问题" \
  -H "Content-Type: application/json"
```

### 响应数据结构

```typescript
interface WorkflowExecutionResult {
  executionId: string;           // 执行ID，唯一标识
  workflowId: number;            // 工作流ID
  success: boolean;              // 是否执行成功
  output: any;                   // 最终输出结果
  executionPath: string[];       // 执行路径（节点ID列表）
  nodeResults: {                 // 各节点执行结果
    [nodeId: string]: NodeExecutionResult;
  };
  totalDurationMs: number;       // 总耗时（毫秒）
  errorMessage: string | null;   // 错误信息（失败时）
  finalVariables: object;        // 最终上下文变量
}

interface NodeExecutionResult {
  nodeId: string;                // 节点ID
  nodeName: string;              // 节点名称
  success: boolean;              // 是否成功
  output: any;                   // 节点输出
  durationMs: number;            // 节点耗时（毫秒）
  errorMessage: string | null;   // 错误信息
  retryCount: number;            // 重试次数
}
```

### 响应示例

#### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "executionId": "wf_1_1713513600000",
    "workflowId": null,
    "success": true,
    "output": "订单ORDER123退款已成功处理，预计3-5个工作日到账",
    "executionPath": [
      "start",
      "query_order",
      "check_refund_eligible",
      "process_refund",
      "rollback_inventory",
      "process_payment_refund",
      "send_notification",
      "generate_response",
      "end"
    ],
    "nodeResults": {
      "query_order": {
        "nodeId": "query_order",
        "nodeName": "查询订单",
        "success": true,
        "output": {"orderId": "ORDER123", "status": "paid", "canRefund": true},
        "durationMs": 1234,
        "errorMessage": null,
        "retryCount": 0
      },
      "check_refund_eligible": {
        "nodeId": "check_refund_eligible",
        "nodeName": "检查退款资格",
        "success": true,
        "output": null,
        "durationMs": 5,
        "errorMessage": null,
        "retryCount": 0
      },
      "rollback_inventory": {
        "nodeId": "rollback_inventory",
        "nodeName": "库存回滚",
        "success": true,
        "output": "库存已回滚",
        "durationMs": 890,
        "errorMessage": null,
        "retryCount": 0
      }
    },
    "totalDurationMs": 5678,
    "errorMessage": null,
    "finalVariables": {
      "order_id": "ORDER123",
      "user_id": 1,
      "refund_reason": "商品质量问题",
      "query_order_result": {...}
    }
  }
}
```

#### 失败响应
```json
{
  "code": 500,
  "message": "系统错误",
  "data": null,
  "description": "工作流执行失败: 工具调用失败: 订单不存在"
}
```

### 前端使用建议

```javascript
// Vue3 示例
async function processOrderRefund(orderId, userId, refundReason) {
  try {
    const response = await axios.post('/api/workflow/order-refund', null, {
      params: {
        orderId,
        userId,
        refundReason
      }
    });
    
    if (response.data.code === 0) {
      const result = response.data.data;
      
      // 显示执行路径
      console.log('执行路径:', result.executionPath);
      
      // 显示各节点状态
      Object.entries(result.nodeResults).forEach(([nodeId, nodeResult]) => {
        console.log(`${nodeResult.nodeName}: ${nodeResult.success ? '✅' : '❌'} (${nodeResult.durationMs}ms)`);
      });
      
      // 显示最终结果
      alert(`处理完成: ${result.output}`);
      
      return result;
    } else {
      throw new Error(response.data.description);
    }
  } catch (error) {
    console.error('工作流执行失败:', error);
    alert('处理失败: ' + error.message);
  }
}
```

---

## 2. 获取订单处理工作流定义

### 接口信息
- **路径**: `GET /api/workflow/order-processing/definition`
- **描述**: 获取订单处理工作流的完整定义，包括节点结构、执行逻辑等
- **认证**: 无需

### 请求参数
无

### 请求示例

```bash
curl http://localhost:8080/api/workflow/order-processing/definition
```

### 响应数据结构

```typescript
interface WorkflowDefinition {
  workflowId: number;            // 工作流ID
  workflowName: string;          // 工作流名称
  description: string;           // 工作流描述
  agentId: number | null;        // 关联的智能体ID
  version: string;               // 版本号
  nodes: WorkflowNode[];         // 节点列表
  startNodeId: string;           // 起始节点ID
  globalVariables: object;       // 全局变量
  timeoutMs: number;             // 超时时间（毫秒）
  enabled: boolean;              // 是否启用
  userId: number;                // 创建者用户ID
}

interface WorkflowNode {
  nodeId: string;                // 节点ID
  nodeName: string;              // 节点名称
  nodeType: NodeType;            // 节点类型
  resourceId: number | null;     // 资源ID（工具或智能体）
  resourceType: ResourceType | null; // 资源类型
  inputParams: object;           // 输入参数映射
  conditionExpression: string | null; // 条件表达式
  trueBranch: string | null;     // 条件为真时的下一节点
  falseBranch: string | null;    // 条件为假时的下一节点
  childNodes: string[] | null;   // 子节点列表
  parallel: boolean | null;      // 是否并行执行
  timeoutMs: number | null;      // 超时时间
  retryCount: number | null;     // 重试次数
  errorStrategy: string | null;  // 错误处理策略
  description: string;           // 节点描述
}

type NodeType = 
  | 'TOOL_CALL'      // 调用工具
  | 'AGENT_CALL'     // 调用智能体
  | 'CONDITION'      // 条件判断
  | 'PARALLEL'       // 并行执行
  | 'SEQUENCE'       // 顺序执行
  | 'START'          // 开始节点
  | 'END';           // 结束节点

type ResourceType = 'TOOL' | 'AGENT';
```

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "workflowId": null,
    "workflowName": "订单退款处理工作流",
    "description": "自动化处理订单查询和退款流程",
    "agentId": null,
    "version": "1.0",
    "nodes": [
      {
        "nodeId": "start",
        "nodeName": "开始",
        "nodeType": "START",
        "resourceId": null,
        "resourceType": null,
        "inputParams": {},
        "conditionExpression": null,
        "trueBranch": null,
        "falseBranch": null,
        "childNodes": null,
        "parallel": null,
        "timeoutMs": null,
        "retryCount": null,
        "errorStrategy": null,
        "description": "工作流入口"
      },
      {
        "nodeId": "query_order",
        "nodeName": "查询订单",
        "nodeType": "TOOL_CALL",
        "resourceId": 1,
        "resourceType": "TOOL",
        "inputParams": {
          "order_id": "${order_id}",
          "user_id": "${user_id}"
        },
        "conditionExpression": null,
        "trueBranch": null,
        "falseBranch": null,
        "childNodes": null,
        "parallel": null,
        "timeoutMs": 10000,
        "retryCount": 2,
        "errorStrategy": "FAIL_FAST",
        "description": "调用订单查询工具获取订单详情"
      },
      {
        "nodeId": "check_refund_eligible",
        "nodeName": "检查退款资格",
        "nodeType": "CONDITION",
        "resourceId": null,
        "resourceType": null,
        "inputParams": {},
        "conditionExpression": "${query_order_result.can_refund}",
        "trueBranch": "process_refund",
        "falseBranch": "reject_refund",
        "childNodes": null,
        "parallel": null,
        "timeoutMs": null,
        "retryCount": null,
        "errorStrategy": null,
        "description": "判断订单是否符合退款条件"
      }
    ],
    "startNodeId": "start",
    "globalVariables": {},
    "timeoutMs": 60000,
    "enabled": true,
    "userId": null
  }
}
```

### 前端使用建议

```javascript
// 可视化工作流编辑器
async function loadWorkflowDefinition() {
  try {
    const response = await axios.get('/api/workflow/order-processing/definition');
    const workflow = response.data.data;
    
    // 渲染工作流图
    renderWorkflowGraph(workflow.nodes, workflow.startNodeId);
    
    // 显示工作流信息
    document.getElementById('workflow-name').textContent = workflow.workflowName;
    document.getElementById('workflow-description').textContent = workflow.description;
    document.getElementById('workflow-version').textContent = workflow.version;
    
  } catch (error) {
    console.error('加载工作流定义失败:', error);
  }
}

// 渲染节点
function renderNode(node) {
  const nodeElement = document.createElement('div');
  nodeElement.className = `workflow-node ${node.nodeType.toLowerCase()}`;
  nodeElement.innerHTML = `
    <div class="node-header">${node.nodeName}</div>
    <div class="node-type">${node.nodeType}</div>
    ${node.description ? `<div class="node-desc">${node.description}</div>` : ''}
    ${node.timeoutMs ? `<div class="node-timeout">超时: ${node.timeoutMs}ms</div>` : ''}
    ${node.retryCount ? `<div class="node-retry">重试: ${node.retryCount}次</div>` : ''}
  `;
  
  return nodeElement;
}
```

---

## 3. 获取并行查询工作流定义

### 接口信息
- **路径**: `GET /api/workflow/parallel-query/definition`
- **描述**: 获取并行数据查询工作流定义，演示并行执行优化
- **认证**: 无需

### 请求参数
无

### 请求示例

```bash
curl http://localhost:8080/api/workflow/parallel-query/definition
```

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "workflowId": null,
    "workflowName": "并行数据查询与推荐工作流",
    "description": "同时查询多个数据源并生成个性化推荐",
    "version": "1.0",
    "nodes": [
      {
        "nodeId": "start",
        "nodeName": "开始",
        "nodeType": "START",
        "description": ""
      },
      {
        "nodeId": "parallel_query",
        "nodeName": "并行查询数据",
        "nodeType": "PARALLEL",
        "childNodes": ["query_user_info", "query_order_history", "query_preferences"],
        "description": "同时查询用户信息、订单历史和偏好设置"
      },
      {
        "nodeId": "query_user_info",
        "nodeName": "查询用户信息",
        "nodeType": "TOOL_CALL",
        "resourceId": 10,
        "resourceType": "TOOL",
        "inputParams": {
          "user_id": "${user_id}"
        },
        "timeoutMs": 5000,
        "description": ""
      }
    ],
    "startNodeId": "start",
    "timeoutMs": 30000,
    "enabled": true
  }
}
```

### 前端使用建议

```javascript
// 展示并行执行优势
function showParallelBenefits(workflow) {
  const parallelNode = workflow.nodes.find(n => n.nodeType === 'PARALLEL');
  
  if (parallelNode) {
    const childCount = parallelNode.childNodes.length;
    const avgTime = 5000; // 假设每个查询5秒
    
    console.log(`并行查询节点: ${parallelNode.nodeName}`);
    console.log(`子任务数量: ${childCount}`);
    console.log(`串行执行耗时: ${childCount * avgTime}ms`);
    console.log(`并行执行耗时: ~${avgTime}ms`);
    console.log(`性能提升: ${((childCount * avgTime - avgTime) / (childCount * avgTime) * 100).toFixed(0)}%`);
  }
}
```

---

## 4. 执行自定义工作流

### 接口信息
- **路径**: `POST /api/workflow/execute`
- **描述**: 执行用户自定义的工作流定义
- **认证**: 需要 userId

### 请求参数

#### Query Parameters
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

#### Request Body
```typescript
interface ExecuteWorkflowRequest {
  workflowDefinition: WorkflowDefinition;  // 工作流定义
  inputParams: {                           // 输入参数
    [key: string]: any;
  };
}
```

### 请求示例

```bash
curl -X POST "http://localhost:8080/api/workflow/execute?userId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowDefinition": {
      "workflowName": "测试工作流",
      "nodes": [...],
      "startNodeId": "start"
    },
    "inputParams": {
      "param1": "value1",
      "param2": "value2"
    }
  }'
```

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "description": "工作流执行功能待集成到决策引擎"
}
```

**注意**: 此接口当前返回模拟数据，实际功能待完善。

---

## 🎨 节点类型说明

### START - 开始节点
工作流的入口点，无实际执行逻辑。

```json
{
  "nodeId": "start",
  "nodeName": "开始",
  "nodeType": "START"
}
```

### END - 结束节点
工作流的出口点，标记执行完成。

```json
{
  "nodeId": "end",
  "nodeName": "结束",
  "nodeType": "END"
}
```

### TOOL_CALL - 工具调用节点
调用系统工具、HTTP工具或MCP工具。

```json
{
  "nodeId": "query_order",
  "nodeName": "查询订单",
  "nodeType": "TOOL_CALL",
  "resourceId": 1,
  "resourceType": "TOOL",
  "inputParams": {
    "order_id": "${order_id}"
  },
  "timeoutMs": 10000,
  "retryCount": 2,
  "errorStrategy": "FAIL_FAST"
}
```

**参数说明**:
- `resourceId`: 工具ID
- `inputParams`: 支持表达式 `${variable_name}` 从上下文获取值
- `timeoutMs`: 超时时间（毫秒）
- `retryCount`: 失败重试次数
- `errorStrategy`: 错误策略（FAIL_FAST/CONTINUE/RETRY）

### AGENT_CALL - 智能体调用节点
调用其他智能体作为子任务。

```json
{
  "nodeId": "generate_response",
  "nodeName": "生成回复",
  "nodeType": "AGENT_CALL",
  "resourceId": 100,
  "resourceType": "AGENT",
  "inputParams": {
    "context": "${query_result}"
  }
}
```

### CONDITION - 条件判断节点
根据条件表达式的结果选择不同分支。

```json
{
  "nodeId": "check_refund",
  "nodeName": "检查退款资格",
  "nodeType": "CONDITION",
  "conditionExpression": "${query_result.can_refund}",
  "trueBranch": "process_refund",
  "falseBranch": "reject_refund"
}
```

**条件表达式**:
- 简单布尔值: `"${can_refund}"` → true/false
- 字符串比较: `"${status} == 'success'"`
- 数值比较: `"${amount} > 100"`

### SEQUENCE - 顺序执行节点
按顺序执行多个子节点。

```json
{
  "nodeId": "process_refund",
  "nodeName": "处理退款",
  "nodeType": "SEQUENCE",
  "childNodes": ["rollback_inventory", "process_payment_refund"]
}
```

**执行逻辑**: step1 → step2 → step3（依次执行）

### PARALLEL - 并行执行节点
同时执行多个子节点，提升性能。

```json
{
  "nodeId": "parallel_query",
  "nodeName": "并行查询",
  "nodeType": "PARALLEL",
  "childNodes": ["query_user", "query_orders", "query_prefs"]
}
```

**执行逻辑**: 同时执行所有子任务，等待全部完成后继续

**性能优势**: 3个5秒的任务并行执行只需5秒，而非15秒

---

## 🔧 错误处理策略

### FAIL_FAST - 快速失败
节点执行失败立即终止整个工作流。

```json
{
  "errorStrategy": "FAIL_FAST"
}
```

**适用场景**: 关键步骤失败无需继续

### CONTINUE - 继续执行
节点失败后记录错误，继续执行后续节点。

```json
{
  "errorStrategy": "CONTINUE"
}
```

**适用场景**: 非关键步骤，允许部分失败

### RETRY - 重试机制
节点失败后自动重试指定次数。

```json
{
  "errorStrategy": "RETRY",
  "retryCount": 3
}
```

**适用场景**: 网络请求、第三方API调用等临时性故障

---

## 📊 前端集成示例

### Vue3 完整示例

```vue
<template>
  <div class="workflow-demo">
    <h2>订单退款处理</h2>
    
    <!-- 输入表单 -->
    <el-form :model="form" label-width="100px">
      <el-form-item label="订单ID">
        <el-input v-model="form.orderId" placeholder="请输入订单ID" />
      </el-form-item>
      <el-form-item label="退款原因">
        <el-input v-model="form.refundReason" placeholder="请输入退款原因" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          提交退款申请
        </el-button>
      </el-form-item>
    </el-form>
    
    <!-- 执行进度 -->
    <div v-if="executionResult" class="execution-result">
      <h3>执行结果</h3>
      
      <!-- 总体状态 -->
      <el-alert
        :title="executionResult.success ? '执行成功' : '执行失败'"
        :type="executionResult.success ? 'success' : 'error'"
        :description="executionResult.output || executionResult.errorMessage"
        show-icon
      />
      
      <!-- 执行路径 -->
      <div class="execution-path">
        <h4>执行路径</h4>
        <el-steps :active="executionPath.length" align-center>
          <el-step
            v-for="(nodeId, index) in executionResult.executionPath"
            :key="nodeId"
            :title="getNodeName(nodeId)"
            :status="getNodeStatus(nodeId)"
          />
        </el-steps>
      </div>
      
      <!-- 节点详情 -->
      <div class="node-details">
        <h4>节点执行详情</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(nodeResult, nodeId) in executionResult.nodeResults"
            :key="nodeId"
            :timestamp="`${nodeResult.durationMs}ms`"
            :type="nodeResult.success ? 'success' : 'danger'"
          >
            <h5>{{ nodeResult.nodeName }}</h5>
            <p>状态: {{ nodeResult.success ? '✅ 成功' : '❌ 失败' }}</p>
            <p v-if="nodeResult.errorMessage">错误: {{ nodeResult.errorMessage }}</p>
            <pre v-if="nodeResult.output">{{ JSON.stringify(nodeResult.output, null, 2) }}</pre>
          </el-timeline-item>
        </el-timeline>
      </div>
      
      <!-- 性能统计 -->
      <div class="performance-stats">
        <h4>性能统计</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总耗时">
            {{ executionResult.totalDurationMs }}ms
          </el-descriptions-item>
          <el-descriptions-item label="执行节点数">
            {{ executionResult.executionPath.length }}
          </el-descriptions-item>
          <el-descriptions-item label="成功节点数">
            {{ getSuccessNodeCount() }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import axios from 'axios';

const form = reactive({
  orderId: '',
  refundReason: '用户申请'
});

const loading = ref(false);
const executionResult = ref(null);

// 提交表单
async function handleSubmit() {
  if (!form.orderId) {
    ElMessage.warning('请输入订单ID');
    return;
  }
  
  loading.value = true;
  executionResult.value = null;
  
  try {
    const response = await axios.post('/api/workflow/order-refund', null, {
      params: {
        orderId: form.orderId,
        userId: 1, // 实际应从登录状态获取
        refundReason: form.refundReason
      }
    });
    
    if (response.data.code === 0) {
      executionResult.value = response.data.data;
      ElMessage.success('退款申请已提交');
    } else {
      ElMessage.error(response.data.description || '提交失败');
    }
  } catch (error) {
    console.error('提交失败:', error);
    ElMessage.error('网络错误，请稍后重试');
  } finally {
    loading.value = false;
  }
}

// 获取节点名称
function getNodeName(nodeId) {
  if (!executionResult.value?.nodeResults[nodeId]) {
    return nodeId;
  }
  return executionResult.value.nodeResults[nodeId].nodeName;
}

// 获取节点状态
function getNodeStatus(nodeId) {
  const nodeResult = executionResult.value?.nodeResults[nodeId];
  if (!nodeResult) return 'wait';
  return nodeResult.success ? 'finish' : 'error';
}

// 获取成功节点数
function getSuccessNodeCount() {
  if (!executionResult.value?.nodeResults) return 0;
  
  return Object.values(executionResult.value.nodeResults)
    .filter(result => result.success)
    .length;
}
</script>

<style scoped>
.workflow-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.execution-result {
  margin-top: 30px;
}

.execution-path {
  margin: 20px 0;
}

.node-details {
  margin: 20px 0;
}

.performance-stats {
  margin: 20px 0;
}
</style>
```

---

## 📝 常见问题

### Q1: 如何查看工作流执行进度？
A: 通过 `executionPath` 字段可以看到已执行的节点顺序，`nodeResults` 包含每个节点的详细状态。

### Q2: 如何处理工作流执行失败？
A: 检查 `success` 字段，如果为 `false`，查看 `errorMessage` 和各个节点的 `errorMessage` 定位问题。

### Q3: 如何优化工作流性能？
A: 使用 `PARALLEL` 节点类型并行执行独立任务，合理设置 `timeoutMs` 避免长时间阻塞。

### Q4: 如何实现条件分支？
A: 使用 `CONDITION` 节点类型，设置 `conditionExpression` 和 `trueBranch`/`falseBranch`。

### Q5: 工作流执行超时怎么办？
A: 检查 `totalDurationMs` 是否接近 `timeoutMs`，优化慢节点或增加超时时间。

---

## 🔗 相关文档

- [工作流使用指南](../WORKFLOW_USAGE_GUIDE.md)
- [工作流增强方案](../WORKFLOW_ENHANCEMENT.md)
- [快速启动指南](../QUICK_START_WORKFLOW.md)

---

**版本**: v1.0  
**更新日期**: 2026-04-19  
**维护者**: abcLiyew
