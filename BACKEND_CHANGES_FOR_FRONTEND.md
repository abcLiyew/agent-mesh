# 后端架构升级与接口变动说明

> **版本**: v2.0  
> **更新日期**: 2026-04-19  
> **适用对象**: 前端开发团队  

---

## 📋 目录

- [一、核心架构变更](#一核心架构变更)
- [二、新增API接口](#二新增api接口)
- [三、接口变动对比](#三接口变动对比)
- [四、数据模型变更](#四数据模型变更)
- [五、前端适配指南](#五前端适配指南)
- [六、迁移示例](#六迁移示例)

---

## 一、核心架构变更

### 1.1 统一智能体引擎（Unified Agent Engine）

**变更说明：**
- ✅ 整合了原有的 `DecisionExecutor`（决策引擎）和 `WorkflowEngine`（工作流引擎）
- ✅ 引入"龙虾架构"理念：长期记忆、技能市场、自适应工作流
- ✅ 提供统一的执行入口 `/api/unified-agent/*`

**影响范围：**
- 前端调用智能体执行的接口路径发生变化
- 支持三种工作流模式：全自动、半自定义、完全自定义

### 1.2 任务规划机制

**新增功能：**
- ✅ 智能体先识别意图，生成待办清单（TODO List）
- ✅ 用户确认后再执行，提升可控性和透明度
- ✅ 支持步骤级别的确认/跳过

**工作流程：**
```
用户输入 → planTask(返回待办清单) → 用户确认 → executePlannedTask(执行)
```

### 1.3 工作流模板系统

**新增功能：**
- ✅ 支持保存和复用工作流模板
- ✅ AI辅助生成工作流（半自定义模式）
- ✅ 三种模式满足不同用户需求

---

## 二、新增API接口

### 2.1 统一智能体引擎接口

**基础路径**: `/api/unified-agent`

#### 2.1.1 执行智能体工作流（同步）

```http
POST /api/unified-agent/execute
Content-Type: application/json
```

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | Long | ✅ | 智能体ID |
| query | String | ✅ | 用户查询内容 |
| workflowId | Long | ❌ | 工作流ID（可选，为空则AI自主决策） |
| context | Map | ❌ | 上下文参数（JSON对象） |

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "success": true,
    "finalResponse": "这是智能体的回答",
    "decisionPath": [...],
    "executionTime": 1234
  },
  "message": "ok"
}
```

---

#### 2.1.2 流式执行智能体工作流（SSE）

```http
GET /api/unified-agent/execute-stream?agentId=1&query=你好
Accept: text/event-stream
```

**说明：**
- 使用 Server-Sent Events 实时推送执行进度
- 适合需要实时反馈的场景（如打字机效果）

**事件格式：**
```
event: progress
data: {"step": "正在思考...", "progress": 30}

event: result
data: {"finalResponse": "完整回答"}
```

---

#### 2.1.3 动态生成工作流

```http
POST /api/unified-agent/generate-workflow?agentId=1&taskDescription=处理订单退款
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "workflowId": 123,
    "message": "工作流生成成功"
  }
}
```

**注意：** 此功能目前为占位实现，后续会完善。

---

### 2.2 任务规划接口（⭐重要新增）

#### 2.2.1 规划任务（返回待办清单）

```http
POST /api/unified-agent/plan-task?agentId=1&query=帮我查询订单ORDER123并申请退款
Content-Type: application/json
```

**请求体（可选）：**
```json
{
  "orderId": "ORDER123"
}
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "taskId": "task_1_1713500000000",
    "taskDescription": "帮我查询订单ORDER123并申请退款",
    "steps": [
      {
        "stepId": "step_1",
        "stepNumber": 1,
        "description": "识别用户意图",
        "stepType": "INTENT_RECOGNITION",
        "estimatedDurationMs": 200,
        "isRequired": true,
        "dependencies": []
      },
      {
        "stepId": "step_2",
        "stepNumber": 2,
        "description": "调用工具: 订单查询工具",
        "stepType": "TOOL_CALL",
        "resourceId": 10,
        "resourceName": "订单查询工具",
        "estimatedDurationMs": 1000,
        "isRequired": true,
        "dependencies": ["step_1"]
      },
      {
        "stepId": "step_3",
        "stepNumber": 3,
        "description": "调用工具: 退款处理工具",
        "stepType": "TOOL_CALL",
        "resourceId": 15,
        "resourceName": "退款处理工具",
        "estimatedDurationMs": 1000,
        "isRequired": false,
        "dependencies": ["step_1"]
      },
      {
        "stepId": "step_4",
        "stepNumber": 4,
        "description": "生成最终回答",
        "stepType": "RESPONSE_GENERATION",
        "estimatedDurationMs": 2000,
        "isRequired": true,
        "dependencies": ["step_1", "step_2", "step_3"]
      }
    ],
    "estimatedDurationMs": 4200,
    "requiresConfirmation": true,
    "agentId": 1,
    "userId": 100,
    "createdAt": 1713500000000
  }
}
```

**前端处理建议：**
1. 展示步骤列表给用户
2. 标记每个步骤的类型、耗时、是否必须
3. 允许用户取消勾选非必须步骤
4. 用户确认后调用 `execute-planned-task`

---

#### 2.2.2 执行已确认的任务计划

```http
POST /api/unified-agent/execute-planned-task?taskId=task_1_1713500000000
Content-Type: application/json
```

**请求体（可选）：**
```json
["step_1", "step_2", "step_4"]
```

**说明：**
- 如果不传 `confirmedSteps`，则执行所有步骤
- 如果传入数组，只执行指定的步骤ID

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "success": true,
    "finalResponse": "订单ORDER123已成功退款",
    "decisionPath": [...],
    "executionTime": 3500
  }
}
```

---

### 2.3 工作流模板接口（⭐重要新增）

#### 2.3.1 创建工作流模板

```http
POST /api/unified-agent/workflow-template/create
Content-Type: application/json
```

**请求体：**
```json
{
  "templateName": "订单退款标准流程",
  "description": "处理订单退款的完整流程",
  "workflowMode": "SEMI_CUSTOM",
  "userDefinedNodes": [
    {
      "nodeName": "查询订单信息",
      "nodeType": "TOOL_CALL",
      "resourceId": 10,
      "resourceName": "订单查询工具",
      "isEditable": true
    }
  ],
  "aiGeneratedNodes": [
    {
      "nodeName": "验证退款条件",
      "nodeType": "CONDITION",
      "isAiGenerated": true,
      "isEditable": true
    }
  ],
  "agentId": 1,
  "isPublic": false
}
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "templateId": 1000,
    "message": "工作流模板创建成功"
  }
}
```

---

#### 2.3.2 AI辅助生成工作流（半自定义）

```http
POST /api/unified-agent/workflow-template/ai-assist?agentId=1&taskDescription=处理订单退款
Content-Type: application/json
```

**请求体（用户定义的节点，可选）：**
```json
[
  {
    "nodeName": "查询订单",
    "nodeType": "TOOL_CALL",
    "resourceId": 10
  }
]
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "templateId": 1001,
    "templateName": "AI辅助: 处理订单退款",
    "workflowMode": "SEMI_CUSTOM",
    "userDefinedNodes": [
      {
        "nodeId": "user_node_1",
        "nodeName": "查询订单",
        "nodeType": "TOOL_CALL",
        "resourceId": 10,
        "isAiGenerated": false,
        "isEditable": true
      }
    ],
    "aiGeneratedNodes": [
      {
        "nodeId": "ai_node_1",
        "nodeName": "验证退款条件",
        "nodeType": "CONDITION",
        "isAiGenerated": true,
        "isEditable": true,
        "description": "AI自动补充的条件判断节点"
      },
      {
        "nodeId": "ai_node_2",
        "nodeName": "调用退款工具",
        "nodeType": "TOOL_CALL",
        "resourceId": 15,
        "isAiGenerated": true,
        "isEditable": true
      },
      {
        "nodeId": "ai_node_3",
        "nodeName": "生成最终回答",
        "nodeType": "RESPONSE_GENERATION",
        "isAiGenerated": true,
        "isEditable": false
      }
    ],
    "allNodes": [...]
  }
}
```

**前端交互流程：**
1. 用户输入任务描述
2. （可选）用户添加关键节点
3. 调用此接口，AI自动生成完整工作流
4. 展示AI生成的节点，标记哪些是AI生成的、是否可编辑
5. 用户确认后保存或直接执行

---

#### 2.3.3 获取工作流模板列表

```http
GET /api/unified-agent/workflow-template/list?mode=SEMI_CUSTOM
```

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| mode | String | ❌ | 工作流模式过滤：FULL_AUTO, SEMI_CUSTOM, FULL_CUSTOM |

**响应示例：**
```json
{
  "code": 0,
  "data": [
    {
      "templateId": 1000,
      "templateName": "订单退款标准流程",
      "workflowMode": "SEMI_CUSTOM",
      "usageCount": 5,
      "rating": 4.5,
      "createdAt": 1713500000000
    }
  ]
}
```

---

#### 2.3.4 基于模板执行工作流

```http
POST /api/unified-agent/workflow-template/execute/1000
Content-Type: application/json
```

**请求体（可选）：**
```json
{
  "orderId": "ORDER123",
  "refundReason": "商品质量问题"
}
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "success": true,
    "finalResponse": "订单退款处理完成",
    "executionTime": 2500
  }
}
```

---

## 三、接口变动对比

### 3.1 原有接口 vs 新接口

| 原接口路径 | 新接口路径 | 状态 | 说明 |
|-----------|-----------|------|------|
| `/api/agent/execute` | `/api/unified-agent/execute` | ⚠️ 兼容 | 旧接口仍可用，建议使用新接口 |
| - | `/api/unified-agent/plan-task` | ✅ 新增 | 任务规划（待办清单） |
| - | `/api/unified-agent/execute-planned-task` | ✅ 新增 | 执行已确认的任务 |
| - | `/api/unified-agent/workflow-template/*` | ✅ 新增 | 工作流模板管理 |
| `/api/workflow/execute` | `/api/unified-agent/execute?workflowId=xxx` | ⚠️ 合并 | 统一到一个接口 |

### 3.2 推荐迁移策略

**阶段1：保持兼容（当前）**
- 旧接口仍然可用
- 新功能使用新接口

**阶段2：逐步迁移（建议）**
- 新页面使用 `/api/unified-agent/*`
- 旧页面逐步改造

**阶段3：完全切换（未来）**
- 废弃旧接口
- 统一使用新接口

---

## 四、数据模型变更

### 4.1 TaskExecutionPlan（任务执行计划）

**用途：** 用于展示待办清单

```typescript
interface TaskExecutionPlan {
  taskId: string;              // 任务ID
  taskDescription: string;     // 任务描述
  steps: TaskStep[];           // 步骤列表
  estimatedDurationMs: number; // 预估总耗时
  requiresConfirmation: boolean; // 是否需要确认
  agentId: number;
  userId: number;
  createdAt: number;
}

interface TaskStep {
  stepId: string;              // 步骤ID
  stepNumber: number;          // 步骤序号
  description: string;         // 步骤描述
  stepType: string;            // 步骤类型
  resourceId?: number;         // 资源ID（工具或智能体）
  resourceName?: string;       // 资源名称
  inputParams?: any;           // 输入参数
  estimatedDurationMs?: number; // 预估耗时
  isRequired: boolean;         // 是否必须
  dependencies: string[];      // 依赖的步骤ID
}
```

**步骤类型枚举：**
- `INTENT_RECOGNITION`: 意图识别
- `TOOL_CALL`: 工具调用
- `AGENT_CALL`: 智能体调用
- `KNOWLEDGE_RETRIEVAL`: 知识库检索
- `CONDITION`: 条件判断
- `RESPONSE_GENERATION`: 回答生成

---

### 4.2 WorkflowTemplate（工作流模板）

**用途：** 保存和复用工作流定义

```typescript
interface WorkflowTemplate {
  templateId?: number;
  templateName: string;
  description: string;
  workflowMode: 'FULL_AUTO' | 'SEMI_CUSTOM' | 'FULL_CUSTOM';
  userDefinedNodes?: TemplateNode[];   // 用户定义的节点
  aiGeneratedNodes?: TemplateNode[];   // AI生成的节点
  allNodes?: TemplateNode[];           // 完整节点列表
  startNodeId?: string;
  agentId: number;
  userId?: number;
  isPublic?: boolean;
  usageCount?: number;
  rating?: number;
  tagsJson?: any;
  createdAt?: number;
  updatedAt?: number;
}

interface TemplateNode {
  nodeId?: string;
  nodeName: string;
  nodeType: string;            // TOOL_CALL, AGENT_CALL, CONDITION等
  resourceId?: number;
  resourceName?: string;
  inputParamsTemplate?: Record<string, any>;
  conditionExpression?: string; // 条件表达式
  childNodeIds?: string[];      // 子节点ID
  nextNodeId?: string;
  isAiGenerated?: boolean;      // 是否AI生成
  isEditable?: boolean;         // 是否可编辑
  description?: string;
  timeoutMs?: number;
  errorStrategy?: 'FAIL_FAST' | 'CONTINUE' | 'RETRY';
}
```

---

## 五、前端适配指南

### 5.1 TypeScript 类型定义

在项目中添加类型定义文件 `types/unified-agent.ts`：

```typescript
// types/unified-agent.ts

export type WorkflowMode = 'FULL_AUTO' | 'SEMI_CUSTOM' | 'FULL_CUSTOM';
export type StepType = 
  | 'INTENT_RECOGNITION'
  | 'TOOL_CALL'
  | 'AGENT_CALL'
  | 'KNOWLEDGE_RETRIEVAL'
  | 'CONDITION'
  | 'RESPONSE_GENERATION';
export type NodeType = 
  | 'TOOL_CALL'
  | 'AGENT_CALL'
  | 'CONDITION'
  | 'SEQUENCE'
  | 'PARALLEL'
  | 'RESPONSE_GENERATION'
  | 'KNOWLEDGE_RETRIEVAL';

export interface TaskStep {
  stepId: string;
  stepNumber: number;
  description: string;
  stepType: StepType;
  resourceId?: number;
  resourceName?: string;
  inputParams?: any;
  estimatedDurationMs?: number;
  isRequired: boolean;
  dependencies: string[];
}

export interface TaskExecutionPlan {
  taskId: string;
  taskDescription: string;
  steps: TaskStep[];
  estimatedDurationMs: number;
  requiresConfirmation: boolean;
  agentId: number;
  userId: number;
  createdAt: number;
}

export interface TemplateNode {
  nodeId?: string;
  nodeName: string;
  nodeType: NodeType;
  resourceId?: number;
  resourceName?: string;
  inputParamsTemplate?: Record<string, any>;
  conditionExpression?: string;
  childNodeIds?: string[];
  nextNodeId?: string;
  isAiGenerated?: boolean;
  isEditable?: boolean;
  description?: string;
  timeoutMs?: number;
  errorStrategy?: 'FAIL_FAST' | 'CONTINUE' | 'RETRY';
}

export interface WorkflowTemplate {
  templateId?: number;
  templateName: string;
  description: string;
  workflowMode: WorkflowMode;
  userDefinedNodes?: TemplateNode[];
  aiGeneratedNodes?: TemplateNode[];
  allNodes?: TemplateNode[];
  startNodeId?: string;
  agentId: number;
  userId?: number;
  isPublic?: boolean;
  usageCount?: number;
  rating?: number;
  tagsJson?: any;
  createdAt?: number;
  updatedAt?: number;
}
```

---

### 5.2 API 封装示例

创建 API 服务类 `services/unifiedAgent.ts`：

```typescript
import axios from 'axios';
import type { 
  TaskExecutionPlan, 
  WorkflowTemplate, 
  DecisionExecutionResult 
} from '@/types/unified-agent';

const BASE_URL = '/api/unified-agent';

export const unifiedAgentApi = {
  /**
   * 执行智能体工作流
   */
  execute(params: {
    agentId: number;
    query: string;
    workflowId?: number;
    context?: Record<string, any>;
  }) {
    return axios.post<{ data: DecisionExecutionResult }>(
      `${BASE_URL}/execute`,
      params.context || {},
      { params: { agentId: params.agentId, query: params.query, workflowId: params.workflowId } }
    );
  },

  /**
   * 规划任务（返回待办清单）
   */
  planTask(params: {
    agentId: number;
    query: string;
    context?: Record<string, any>;
  }) {
    return axios.post<{ data: TaskExecutionPlan }>(
      `${BASE_URL}/plan-task`,
      params.context || {},
      { params: { agentId: params.agentId, query: params.query } }
    );
  },

  /**
   * 执行已确认的任务计划
   */
  executePlannedTask(taskId: string, confirmedSteps?: string[]) {
    return axios.post<{ data: DecisionExecutionResult }>(
      `${BASE_URL}/execute-planned-task`,
      confirmedSteps || null,
      { params: { taskId } }
    );
  },

  /**
   * AI辅助生成工作流
   */
  aiAssistWorkflow(params: {
    agentId: number;
    taskDescription: string;
    userDefinedNodes?: any[];
  }) {
    return axios.post<{ data: WorkflowTemplate }>(
      `${BASE_URL}/workflow-template/ai-assist`,
      params.userDefinedNodes || [],
      { params: { agentId: params.agentId, taskDescription: params.taskDescription } }
    );
  },

  /**
   * 创建工作流模板
   */
  createWorkflowTemplate(template: WorkflowTemplate) {
    return axios.post<{ data: { templateId: number } }>(
      `${BASE_URL}/workflow-template/create`,
      template
    );
  },

  /**
   * 获取工作流模板列表
   */
  getWorkflowTemplates(mode?: string) {
    return axios.get<{ data: WorkflowTemplate[] }>(
      `${BASE_URL}/workflow-template/list`,
      { params: { mode } }
    );
  },

  /**
   * 基于模板执行工作流
   */
  executeFromTemplate(templateId: number, inputParams?: Record<string, any>) {
    return axios.post<{ data: DecisionExecutionResult }>(
      `${BASE_URL}/workflow-template/execute/${templateId}`,
      inputParams || {}
    );
  },
};
```

---

### 5.3 Vue3 组件示例

#### 示例1：任务规划与执行组件

```vue
<template>
  <div class="task-planner">
    <h3>🤖 智能体任务规划</h3>
    
    <!-- 输入框 -->
    <div class="input-section">
      <textarea 
        v-model="userQuery" 
        placeholder="请输入您的需求..."
      />
      <button @click="handlePlanTask" :disabled="isPlanning">
        {{ isPlanning ? '规划中...' : '📋 生成待办清单' }}
      </button>
    </div>

    <!-- 待办清单展示 -->
    <div v-if="taskPlan" class="todo-list">
      <h4>📝 待办清单（预估耗时：{{ formatDuration(taskPlan.estimatedDurationMs) }}）</h4>
      
      <div 
        v-for="step in taskPlan.steps" 
        :key="step.stepId"
        class="step-item"
        :class="{ 'step-required': step.isRequired }"
      >
        <label>
          <input 
            type="checkbox" 
            :checked="selectedSteps.includes(step.stepId)"
            :disabled="!step.isRequired"
            @change="toggleStep(step.stepId)"
          />
          <span class="step-number">{{ step.stepNumber }}</span>
          <span class="step-desc">{{ step.description }}</span>
          <span class="step-type">{{ getStepTypeLabel(step.stepType) }}</span>
          <span v-if="step.resourceName" class="step-resource">
            🔧 {{ step.resourceName }}
          </span>
          <span class="step-duration">⏱️ {{ step.estimatedDurationMs }}ms</span>
          <span v-if="!step.isRequired" class="step-optional">（可选）</span>
        </label>
      </div>

      <div class="actions">
        <button @click="handleExecute" :disabled="isExecuting">
          {{ isExecuting ? '执行中...' : '▶️ 开始执行' }}
        </button>
        <button @click="reset">重新规划</button>
      </div>
    </div>

    <!-- 执行结果 -->
    <div v-if="executionResult" class="result">
      <h4>✅ 执行结果</h4>
      <div class="response">{{ executionResult.finalResponse }}</div>
      <div class="meta">
        耗时: {{ executionResult.executionTime }}ms | 
        成功: {{ executionResult.success ? '是' : '否' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { unifiedAgentApi } from '@/services/unifiedAgent';
import type { TaskExecutionPlan, DecisionExecutionResult } from '@/types/unified-agent';

const userQuery = ref('');
const taskPlan = ref<TaskExecutionPlan | null>(null);
const selectedSteps = ref<string[]>([]);
const executionResult = ref<DecisionExecutionResult | null>(null);
const isPlanning = ref(false);
const isExecuting = ref(false);

const handlePlanTask = async () => {
  if (!userQuery.value.trim()) return;
  
  isPlanning.value = true;
  try {
    const response = await unifiedAgentApi.planTask({
      agentId: 1,
      query: userQuery.value
    });
    
    taskPlan.value = response.data.data;
    // 默认选中所有必须步骤
    selectedSteps.value = taskPlan.value.steps
      .filter(s => s.isRequired)
      .map(s => s.stepId);
  } catch (error) {
    console.error('任务规划失败', error);
    alert('任务规划失败，请重试');
  } finally {
    isPlanning.value = false;
  }
};

const toggleStep = (stepId: string) => {
  const index = selectedSteps.value.indexOf(stepId);
  if (index > -1) {
    selectedSteps.value.splice(index, 1);
  } else {
    selectedSteps.value.push(stepId);
  }
};

const handleExecute = async () => {
  if (!taskPlan.value) return;
  
  isExecuting.value = true;
  try {
    const response = await unifiedAgentApi.executePlannedTask(
      taskPlan.value.taskId,
      selectedSteps.value
    );
    
    executionResult.value = response.data.data;
  } catch (error) {
    console.error('执行失败', error);
    alert('执行失败，请重试');
  } finally {
    isExecuting.value = false;
  }
};

const reset = () => {
  taskPlan.value = null;
  selectedSteps.value = [];
  executionResult.value = null;
};

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
};

const getStepTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    INTENT_RECOGNITION: '🧠 意图识别',
    TOOL_CALL: '🔧 工具调用',
    AGENT_CALL: '🤖 智能体调用',
    KNOWLEDGE_RETRIEVAL: '📚 知识检索',
    CONDITION: '⚖️ 条件判断',
    RESPONSE_GENERATION: '💬 回答生成'
  };
  return labels[type] || type;
};
</script>

<style scoped>
.todo-list {
  margin-top: 20px;
}

.step-item {
  padding: 12px;
  margin: 8px 0;
  background: #f9f9f9;
  border-radius: 8px;
  border-left: 4px solid #ddd;
}

.step-item.step-required {
  border-left-color: #4CAF50;
  background: #e8f5e9;
}

.step-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #2196F3;
  color: white;
  border-radius: 50%;
  margin-right: 8px;
  font-size: 0.9em;
}

.step-type {
  margin-left: 8px;
  padding: 2px 8px;
  background: #e3f2fd;
  border-radius: 4px;
  font-size: 0.85em;
}

.step-resource {
  margin-left: 8px;
  color: #666;
  font-size: 0.9em;
}

.step-duration {
  margin-left: 8px;
  color: #999;
  font-size: 0.85em;
}

.step-optional {
  color: #ff9800;
  font-size: 0.85em;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}
</style>
```

---

#### 示例2：AI辅助工作流编辑器

```vue
<template>
  <div class="workflow-creator">
    <h3>🛠️ AI辅助工作流编辑器</h3>
    
    <!-- 任务描述 -->
    <div class="task-input">
      <label>任务描述：</label>
      <textarea 
        v-model="taskDescription" 
        placeholder="例如：处理订单退款，需要先查询订单，然后验证条件，最后执行退款"
      />
    </div>

    <!-- 用户定义的节点 -->
    <div class="user-nodes">
      <h4>您定义的关键节点：</h4>
      <div v-for="(node, index) in userNodes" :key="index" class="node-card">
        <input v-model="node.nodeName" placeholder="节点名称" />
        <select v-model="node.nodeType">
          <option value="TOOL_CALL">工具调用</option>
          <option value="AGENT_CALL">智能体调用</option>
          <option value="CONDITION">条件判断</option>
        </select>
        <button @click="removeNode(index)">❌</button>
      </div>
      <button @click="addNode" class="btn-add">➕ 添加节点</button>
    </div>

    <!-- AI生成按钮 -->
    <button 
      @click="handleAiAssist" 
      :disabled="isGenerating || !taskDescription"
      class="btn-primary"
    >
      {{ isGenerating ? '🤖 AI生成中...' : '🤖 AI辅助生成工作流' }}
    </button>

    <!-- AI生成的节点预览 -->
    <div v-if="generatedTemplate" class="ai-nodes">
      <h4>🤖 AI补充的节点：</h4>
      <div 
        v-for="node in generatedTemplate.aiGeneratedNodes" 
        :key="node.nodeId"
        class="node-card ai-node"
      >
        <span v-if="node.isAiGenerated" class="ai-badge">AI生成</span>
        <strong>{{ node.nodeName }}</strong>
        <p>{{ node.description }}</p>
        <span v-if="!node.isEditable" class="locked">🔒 不可编辑</span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="generatedTemplate" class="actions">
      <button @click="handleSave" class="btn-success">💾 保存模板</button>
      <button @click="handleExecute" class="btn-primary">▶️ 立即执行</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { unifiedAgentApi } from '@/services/unifiedAgent';
import type { WorkflowTemplate, TemplateNode } from '@/types/unified-agent';

const taskDescription = ref('');
const userNodes = ref<TemplateNode[]>([]);
const generatedTemplate = ref<WorkflowTemplate | null>(null);
const isGenerating = ref(false);

const addNode = () => {
  userNodes.value.push({
    nodeName: '',
    nodeType: 'TOOL_CALL',
    isAiGenerated: false,
    isEditable: true
  });
};

const removeNode = (index: number) => {
  userNodes.value.splice(index, 1);
};

const handleAiAssist = async () => {
  if (!taskDescription.value.trim()) return;
  
  isGenerating.value = true;
  try {
    const response = await unifiedAgentApi.aiAssistWorkflow({
      agentId: 1,
      taskDescription: taskDescription.value,
      userDefinedNodes: userNodes.value
    });
    
    generatedTemplate.value = response.data.data;
  } catch (error) {
    console.error('AI生成失败', error);
    alert('AI生成失败，请重试');
  } finally {
    isGenerating.value = false;
  }
};

const handleSave = async () => {
  if (!generatedTemplate.value) return;
  
  try {
    const response = await unifiedAgentApi.createWorkflowTemplate(generatedTemplate.value);
    alert(`模板保存成功！ID: ${response.data.data.templateId}`);
  } catch (error) {
    console.error('保存失败', error);
    alert('保存失败，请重试');
  }
};

const handleExecute = async () => {
  if (!generatedTemplate.value?.templateId) return;
  
  try {
    const response = await unifiedAgentApi.executeFromTemplate(
      generatedTemplate.value.templateId,
      {}
    );
    
    alert('执行成功！');
    console.log('执行结果:', response.data.data);
  } catch (error) {
    console.error('执行失败', error);
    alert('执行失败，请重试');
  }
};
</script>

<style scoped>
.workflow-creator {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.task-input textarea {
  width: 100%;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.node-card {
  padding: 15px;
  margin: 10px 0;
  background: #f9f9f9;
  border-radius: 8px;
  border-left: 4px solid #4CAF50;
}

.ai-node {
  border-left-color: #2196F3;
  background: #e3f2fd;
}

.ai-badge {
  background: #2196F3;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8em;
  margin-right: 10px;
}

.locked {
  color: #999;
  font-size: 0.9em;
}

.btn-primary {
  background: #2196F3;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-success {
  background: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
```

---

## 六、迁移示例

### 6.1 场景1：简单对话（无需改动）

**旧代码：**
```javascript
// 调用原有接口
axios.post('/api/agent/execute', {
  agentId: 1,
  query: '你好'
});
```

**新代码（推荐）：**
```javascript
// 使用统一引擎
unifiedAgentApi.execute({
  agentId: 1,
  query: '你好'
});
```

---

### 6.2 场景2：复杂任务（推荐使用任务规划）

**需求：** 用户希望知道智能体将要执行什么操作

**新实现：**
```javascript
// 第1步：规划任务
const planResponse = await unifiedAgentApi.planTask({
  agentId: 1,
  query: '帮我查询订单ORDER123并申请退款'
});

const taskPlan = planResponse.data.data;

// 第2步：展示待办清单给用户
displayTodoList(taskPlan.steps);

// 第3步：用户确认后执行
const confirmSteps = getUserConfirmedSteps(); // 用户选择的步骤ID
const result = await unifiedAgentApi.executePlannedTask(
  taskPlan.taskId,
  confirmSteps
);
```

---

### 6.3 场景3：标准化流程（使用工作流模板）

**需求：** 订单退款是高频操作，希望保存为标准流程

**实现：**
```javascript
// 第1步：AI辅助生成工作流
const aiResponse = await unifiedAgentApi.aiAssistWorkflow({
  agentId: 1,
  taskDescription: '订单退款流程',
  userDefinedNodes: [
    {
      nodeName: '查询订单',
      nodeType: 'TOOL_CALL',
      resourceId: 10
    }
  ]
});

const template = aiResponse.data.data;

// 第2步：用户确认后保存
const saveResponse = await unifiedAgentApi.createWorkflowTemplate(template);
const templateId = saveResponse.data.data.templateId;

// 第3步：后续直接使用模板
const result = await unifiedAgentApi.executeFromTemplate(templateId, {
  orderId: 'ORDER123',
  refundReason: '商品质量问题'
});
```

---

## 七、常见问题

### Q1: 旧接口还能用吗？
**A:** 可以，旧接口仍然兼容，但建议逐步迁移到新接口以获得更好的功能和性能。

### Q2: 任务规划的缓存多久过期？
**A:** 目前设置为5分钟过期。生产环境建议使用Redis持久化。

### Q3: 工作流模板存储在哪里？
**A:** 目前使用内存缓存，重启后丢失。生产环境需要迁移到数据库。

### Q4: 如何实现真正的逐步执行？
**A:** 当前版本简化处理，直接调用决策引擎。后续版本会实现真正的节点级逐步执行和进度推送。

### Q5: SSE流式执行如何使用？
**A:** 参考前端示例中的EventSource用法，监听`progress`和`result`事件。

---

## 八、技术支持

如有疑问，请联系后端开发团队或查阅以下文档：
- Swagger文档: `http://localhost:8080/swagger-ui.html`
- 工作流模板指南: `WORKFLOW_TEMPLATE_GUIDE.md`
- 任务规划指南: `TASK_PLANNING_GUIDE.md`

---

**文档版本**: v1.0  
**最后更新**: 2026-04-19  
**维护者**: 后端开发团队
