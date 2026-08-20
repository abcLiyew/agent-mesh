# Agent Mesh 完整 API 接口文档

> **版本**: v2.0  
> **更新日期**: 2026-04-19  
> **基础URL**: `http://localhost:8080/api/v1`  
> **认证方式**: Session Cookie（登录后自动携带）

---

## 📋 目录

- [一、用户认证模块](#一用户认证模块)
- [二、智能体管理模块](#二智能体管理模块)
- [三、统一智能体引擎](#三统一智能体引擎)
- [四、工作流管理模块](#四工作流管理模块)
- [五、工具管理模块](#五工具管理模块)
- [六、RAG知识库模块](#六rag知识库模块)
- [七、长期记忆模块](#七长期记忆模块)
- [八、技能市场模块](#八技能市场模块)
- [九、反馈与分析模块](#九反馈与分析模块)
- [十、系统管理模块](#十系统管理模块)

---

## 一、用户认证模块

### 1.1 用户登录

**接口**: `POST /users/login`

**请求参数**:
```json
{
  "userAccount": "admin",
  "userPassword": "password123"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userName": "管理员",
    "userRole": "ADMIN",
    "createdAt": "2026-01-01T00:00:00"
  },
  "message": "登录成功"
}
```

**说明**: 登录成功后，Session中会存储用户信息，后续请求会自动携带Cookie。

---

### 1.2 用户登出

**接口**: `POST /users/logout`

**响应**:
```json
{
  "code": 200,
  "message": "登出成功"
}
```

---

### 1.3 获取当前用户信息

**接口**: `GET /users/current`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userName": "管理员",
    "userRole": "ADMIN",
    "email": "admin@example.com",
    "createdAt": "2026-01-01T00:00:00"
  }
}
```

---

### 1.4 用户注册

**接口**: `POST /users/register`

**请求参数**:
```json
{
  "userName": "新用户",
  "userAccount": "newuser",
  "userPassword": "password123",
  "email": "newuser@example.com"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "userId": 2
  },
  "message": "注册成功"
}
```

---

## 二、智能体管理模块

### 2.1 创建智能体

**接口**: `POST /agents`

**请求参数**:
```json
{
  "name": "客服助手",
  "description": "专业的客户服务智能体",
  "roleDefinition": "你是一名专业的客服代表，负责解答用户问题",
  "personalityTraits": "友好、耐心、专业",
  "defaultModelId": 1,
  "visibility": "PUBLIC",
  "tags": ["客服", "服务"]
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "agentId": 100
  }
}
```

---

### 2.2 获取智能体列表

**接口**: `GET /agents?page={page}&pageSize={size}&keyword={text}`

**查询参数**:
- `page`: 页码（默认1）
- `pageSize`: 每页数量（默认10）
- `keyword`: 搜索关键词（可选）

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 100,
        "name": "客服助手",
        "description": "专业的客户服务智能体",
        "roleDefinition": "你是一名专业的客服代表...",
        "ownerId": 1,
        "visibility": "PUBLIC",
        "usageCount": 150,
        "rating": 4.5,
        "createdAt": "2026-01-15T10:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10
  }
}
```

---

### 2.3 获取智能体详情

**接口**: `GET /agents/{agentId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 100,
    "name": "客服助手",
    "description": "专业的客户服务智能体",
    "roleDefinition": "你是一名专业的客服代表...",
    "personalityTraits": "友好、耐心、专业",
    "defaultModelId": 1,
    "ownerId": 1,
    "visibility": "PUBLIC",
    "tags": ["客服", "服务"],
    "usageCount": 150,
    "rating": 4.5,
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-04-19T08:00:00"
  }
}
```

---

### 2.4 更新智能体

**接口**: `PUT /agents/{agentId}`

**请求参数**:
```json
{
  "name": "客服助手V2",
  "description": "升级版的客户服务智能体",
  "roleDefinition": "你是一名资深的客服专家...",
  "personalityTraits": "专业、高效、温暖",
  "defaultModelId": 2,
  "visibility": "PRIVATE",
  "tags": ["客服", "高级"]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 2.5 删除智能体（软删除）

**接口**: `DELETE /agents/{agentId}`

**响应**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 2.6 获取智能体依赖关系

**接口**: `GET /agents/{agentId}/dependencies`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "dependencyId": 1,
      "agentId": 100,
      "dependsOnAgentId": 200,
      "dependencyType": "CALL",
      "priority": 1,
      "dependentAgent": {
        "id": 200,
        "name": "技术评估专家",
        "roleDefinition": "TECH_EXPERT"
      }
    }
  ]
}
```

---

### 2.7 添加智能体依赖

**接口**: `POST /agents/{agentId}/dependencies`

**请求参数**:
```json
{
  "dependsOnAgentId": 200,
  "dependencyType": "CALL",
  "priority": 1
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "dependencyId": 1
  }
}
```

---

### 2.8 移除智能体依赖

**接口**: `DELETE /agents/{agentId}/dependencies/{dependencyId}`

**响应**:
```json
{
  "code": 200,
  "message": "移除成功"
}
```

---

## 三、统一智能体引擎

### 3.1 执行智能体任务

**接口**: `POST /unified-agent/execute`

**请求参数**:
```json
{
  "agentId": 100,
  "query": "帮我分析一下最近的市场趋势",
  "workflowId": null,
  "context": {
    "kbIds": [1, 2],
    "extraParam": "value"
  }
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "finalResponse": "根据分析，最近市场呈现以下趋势...",
    "success": true,
    "executionTimeMs": 3500,
    "decisionPath": [
      {
        "stepId": "step_1",
        "stepType": "TOOL_CALL",
        "description": "调用市场分析工具",
        "status": "COMPLETED",
        "durationMs": 1200
      },
      {
        "stepId": "step_2",
        "stepType": "RESPONSE_GENERATION",
        "description": "生成分析报告",
        "status": "COMPLETED",
        "durationMs": 2300
      }
    ],
    "callChainTrace": {
      "rootAgentId": 100,
      "callRecords": [
        {
          "agentId": 100,
          "calledAt": "2026-04-19T10:00:00",
          "durationMs": 3500
        }
      ]
    },
    "performanceStats": {
      "totalCalls": 2,
      "successCount": 2,
      "avgExecutionTimeMs": 1750
    }
  }
}
```

**特性**:
- 🔄 自动判断任务复杂度，选择执行策略
- 🧠 检索长期记忆增强上下文
- 📊 记录完整的调用链和性能统计
- 🎯 支持指定workflowId强制使用工作流

---

### 3.2 异步执行智能体任务

**接口**: `POST /unified-agent/execute-async`

**请求参数**: 同 3.1

**响应**:
```json
{
  "code": 200,
  "message": "任务已提交到后台执行"
}
```

---

### 3.3 流式执行智能体任务 (SSE)

**接口**: `POST /unified-agent/execute-stream`

**请求参数**:
```json
{
  "agentId": 123,
  "query": "帮我分析一下市场趋势",
  "workflowId": null,
  "context": {
    "kbIds": [1, 2]
  }
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应类型**: `text/event-stream`

**SSE事件流示例**:
```
event: planning
data: {"message":"正在分析任务...","progress":10}

event: plan_ready
data: {"taskId":"task_100_1713500000000","totalSteps":3,"steps":[...],"progress":20}

event: step_start
data: {"stepNumber":1,"totalSteps":3,"description":"调用市场分析工具","progress":40}

event: step_complete
data: {"stepNumber":1,"result":{"marketData":{}},"progress":50}

event: generating_response
data: {"message":"正在生成最终回答...","progress":95}

event: complete
data: {"success":true,"finalResponse":"根据分析...","executionTimeMs":3500,"progress":100}
```

---

### 3.4 AI任务规划

**接口**: `POST /unified-agent/plan-task`

**请求参数**:
```json
{
  "agentId": 100,
  "query": "帮我对比三个产品的优缺点",
  "context": {}
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "taskId": "task_100_1713500000000",
    "taskDescription": "帮我对比三个产品的优缺点",
    "steps": [
      {
        "stepId": "step_1",
        "stepType": "KNOWLEDGE_RETRIEVAL",
        "description": "检索产品A的相关信息",
        "resourceId": 1,
        "estimatedDurationMs": 2000,
        "inputParams": {"topK": 3}
      },
      {
        "stepId": "step_2",
        "stepType": "TOOL_CALL",
        "description": "调用产品对比工具",
        "resourceId": 5,
        "estimatedDurationMs": 3000
      },
      {
        "stepId": "step_3",
        "stepType": "RESPONSE_GENERATION",
        "description": "生成对比报告",
        "estimatedDurationMs": 2000
      }
    ],
    "estimatedDurationMs": 7000,
    "requiresConfirmation": true
  }
}
```

---

### 3.5 执行已确认的任务计划

**接口**: `POST /unified-agent/execute-planned`

**请求参数**:
```json
{
  "taskId": "task_100_1713500000000",
  "confirmedSteps": ["step_1", "step_2", "step_3"]
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**: 同 3.1 execute接口

---

### 3.6 学习和优化

**接口**: `POST /unified-agent/learn-and-optimize`

**请求参数**:
```json
{
  "workflowId": 789,
  "executionResult": {
    "success": true,
    "executionTimeMs": 3500,
    "decisionPath": [...]
  },
  "userFeedback": "回答很好，但速度有点慢"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "学习优化完成"
}
```

---

### 3.7 创建工作流模板

**接口**: `POST /unified-agent/workflow-template`

**请求参数**:
```json
{
  "templateName": "市场分析工作流",
  "description": "自动化市场趋势分析",
  "workflowMode": "SEMI_CUSTOM",
  "userDefinedNodes": [
    {
      "nodeId": "node_1",
      "nodeName": "获取市场数据",
      "nodeType": "TOOL_CALL",
      "resourceId": 10
    }
  ],
  "agentId": 100,
  "isPublic": false
}
```

**注意**: userId和authorId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "templateId": 1000
  }
}
```

---

### 3.8 AI辅助生成工作流

**接口**: `POST /unified-agent/ai-assist-workflow`

**请求参数**:
```json
{
  "agentId": 100,
  "taskDescription": "帮我分析市场趋势并生成报告",
  "userDefinedNodes": [
    {
      "nodeId": "node_1",
      "nodeName": "用户自定义节点",
      "nodeType": "TOOL_CALL",
      "resourceId": 10
    }
  ]
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "templateId": 1001,
    "templateName": "AI辅助: 帮我分析市场趋势并生成报告",
    "workflowMode": "SEMI_CUSTOM",
    "userDefinedNodes": [...],
    "aiGeneratedNodes": [
      {
        "nodeId": "ai_node_1",
        "nodeName": "检索知识库",
        "nodeType": "KNOWLEDGE_RETRIEVAL",
        "isAiGenerated": true,
        "isEditable": false
      },
      {
        "nodeId": "ai_node_2",
        "nodeName": "生成最终回答",
        "nodeType": "RESPONSE_GENERATION",
        "isAiGenerated": true
      }
    ],
    "allNodes": [...]
  }
}
```

---

### 3.9 获取工作流模板列表

**接口**: `GET /unified-agent/workflow-templates?mode={mode}`

**查询参数**:
- `mode`: 工作流模式（可选：FULL_AUTO/SEMI_CUSTOM/FULL_CUSTOM）

**注意**: userId从Session中自动获取，无需在URL中传递

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "templateId": 1000,
      "templateName": "市场分析工作流",
      "workflowMode": "SEMI_CUSTOM",
      "usageCount": 5,
      "createdAt": "2026-04-15T10:00:00"
    }
  ]
}
```

---

### 3.10 基于模板执行工作流

**接口**: `POST /unified-agent/execute-from-template`

**请求参数**:
```json
{
  "templateId": 1000,
  "inputParams": {
    "marketRegion": "Asia",
    "timeRange": "last_30_days"
  }
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**: 同 3.1 execute接口

---

### 3.11 多智能体协同执行

**接口**: `POST /unified-agent/execute-collaboratively`

**请求参数**:
```json
{
  "mainAgentId": 100,
  "query": "综合分析这个项目的技术可行性和市场前景",
  "context": {}
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "mergedResult": {
      "finalResponse": "### 多智能体协同执行结果\n\n**执行统计**: 总计 3 个智能体, 成功 3 个, 失败 0 个\n\n#### 1. 技术评估专家\n...\n\n#### 2. 市场分析师\n...\n\n### 总结\n..."
    },
    "participatingAgents": [
      {
        "agentId": 200,
        "agentName": "技术评估专家",
        "role": "TECH_EXPERT"
      },
      {
        "agentId": 201,
        "agentName": "市场分析师",
        "role": "MARKET_ANALYST"
      }
    ],
    "subAgentResults": [...],
    "totalExecutionTimeMs": 5000
  }
}
```

---

## 四、工作流管理模块

### 4.1 创建工作流

**接口**: `POST /api/workflow/create`

**请求参数**:
```json
{
  "workflowName": "数据分析工作流",
  "description": "自动分析数据并生成报告",
  "agentId": 123,
  "version": "1.0.0",
  "nodesJson": {
    "nodes": [
      {
        "nodeId": "node_1",
        "nodeName": "获取数据",
        "nodeType": "TOOL_CALL",
        "resourceId": 10
      },
      {
        "nodeId": "node_2",
        "nodeName": "分析数据",
        "nodeType": "KNOWLEDGE_RETRIEVAL",
        "resourceId": 5
      }
    ],
    "edges": [
      {"from": "node_1", "to": "node_2"}
    ]
  },
  "startNodeId": "node_1",
  "globalVariablesJson": {
    "timeout": 30000
  },
  "timeoutMs": 30000,
  "enabled": true
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": 1000
}
```

---

### 4.2 更新工作流

**接口**: `PUT /api/workflow/{workflowId}`

**请求参数**: 同创建工作流（不包含userId字段）

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

---

### 4.3 删除工作流

**接口**: `DELETE /api/workflow/{workflowId}`

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

**说明**: 软删除，不会真正从数据库删除

---

### 4.4 获取工作流详情

**接口**: `GET /api/workflow/{workflowId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1000,
    "workflowName": "数据分析工作流",
    "description": "自动分析数据并生成报告",
    "agentId": 123,
    "version": "1.0.0",
    "nodesJson": {...},
    "startNodeId": "node_1",
    "globalVariablesJson": {...},
    "timeoutMs": 30000,
    "enabled": true,
    "userId": 456,
    "createdAt": "2026-04-19T10:00:00",
    "updatedAt": "2026-04-19T10:00:00"
  }
}
```

---

### 4.5 获取我的工作流列表

**接口**: `GET /api/workflow/my-workflows?page={n}&pageSize={n}`

**查询参数**:
- `page`: 页码（默认1）
- `pageSize`: 每页数量（默认10）

**注意**: userId从Session中自动获取，无需在URL中传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1000,
        "workflowName": "数据分析工作流",
        "description": "自动分析数据并生成报告",
        "agentId": 123,
        "version": "1.0.0",
        "enabled": true,
        "createdAt": "2026-04-19T10:00:00",
        "updatedAt": "2026-04-19T10:00:00"
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

### 4.6 获取智能体的工作流列表

**接口**: `GET /api/workflow/agent/{agentId}`

**响应**: 返回该智能体关联的所有已启用工作流

---

### 4.7 启用/禁用工作流

**接口**: `PUT /api/workflow/{workflowId}/toggle?enabled={true/false}`

**查询参数**:
- `enabled`: true=启用, false=禁用

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

---

### 4.8 执行订单退款工作流（示例）

**接口**: `POST /api/workflow/order-refund?orderId={id}&refundReason={text}`

**查询参数**:
- `orderId`: 订单ID
- `refundReason`: 退款原因（可选）

**响应**:
```json
{
  "code": 200,
  "data": {
    "executionId": "exec_500_1713500000000",
    "status": "COMPLETED",
    "output": {...},
    "nodeResults": {
      "node_1": {
        "nodeId": "node_1",
        "nodeName": "读取数据",
        "success": true,
        "output": {...},
        "durationMs": 1200
      }
    },
    "executionPath": ["node_1", "node_2"],
    "totalDurationMs": 3500
  }
}
```

---

## 五、工具管理模块

### 5.1 创建工具

**接口**: `POST /tools`

**请求参数**:
```json
{
  "toolCodeName": "custom_api_tool",
  "displayName": "自定义API工具",
  "description": "调用外部API获取数据",
  "sourceType": "USER_HTTP",
  "inputSchema": {
    "type": "object",
    "required": ["url"],
    "properties": {
      "url": {"type": "string", "description": "API地址"},
      "method": {"type": "string", "enum": ["GET", "POST"]}
    }
  },
  "customEndpointUrl": "https://api.example.com/data",
  "isEnabled": true
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "toolId": 10
  }
}
```

---

### 5.2 获取工具列表

**接口**: `GET /tools?page={page}&pageSize={size}&sourceType={type}`

**查询参数**:
- `page`: 页码（默认1）
- `pageSize`: 每页数量（默认10）
- `sourceType`: 工具来源（可选：SYSTEM/USER_HTTP/USER_MCP/USER_AGENT）

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 10,
        "toolCodeName": "custom_api_tool",
        "displayName": "自定义API工具",
        "description": "调用外部API获取数据",
        "sourceType": "USER_HTTP",
        "isEnabled": true,
        "healthStatus": 1,
        "createdAt": "2026-04-01T10:00:00"
      }
    ],
    "total": 25,
    "current": 1,
    "size": 10
  }
}
```

---

### 5.3 获取工具详情

**接口**: `GET /tools/{toolId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 10,
    "toolCodeName": "custom_api_tool",
    "displayName": "自定义API工具",
    "description": "调用外部API获取数据",
    "sourceType": "USER_HTTP",
    "inputSchema": {...},
    "outputSchema": {...},
    "customEndpointUrl": "https://api.example.com/data",
    "isEnabled": true,
    "healthStatus": 1,
    "consecutiveFailures": 0,
    "lastErrorMessage": null,
    "createdAt": "2026-04-01T10:00:00"
  }
}
```

---

### 5.4 更新工具

**接口**: `PUT /tools/{toolId}`

**请求参数**: 同 5.1 创建接口

**响应**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 5.5 删除工具

**接口**: `DELETE /tools/{toolId}`

**响应**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 5.6 调用工具

**接口**: `POST /tools/invoke`

**请求参数**:
```json
{
  "toolId": 10,
  "parameters": {
    "url": "https://api.example.com/data",
    "method": "GET"
  }
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "result": "{...}",
    "toolId": 10,
    "toolName": "自定义API工具",
    "executionTime": 1713500000000,
    "executionTimeMs": 150
  }
}
```

---

### 5.7 手动触发工具健康检查

**接口**: `POST /tools/{toolId}/health-check`

**响应**:
```json
{
  "code": 200,
  "data": {
    "healthStatus": 1,
    "message": "工具健康检查通过"
  }
}
```

---

## 六、RAG知识库模块

### 6.1 创建知识库

**接口**: `POST /knowledge-bases`

**请求参数**:
```json
{
  "name": "产品文档库",
  "description": "存储产品相关文档",
  "visibility": "PRIVATE",
  "embeddingModelId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "kbId": 1
  }
}
```

---

### 6.2 获取知识库列表

**接口**: `GET /knowledge-bases?page={page}&pageSize={size}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "产品文档库",
        "description": "存储产品相关文档",
        "documentCount": 50,
        "visibility": "PRIVATE",
        "createdAt": "2026-03-15T10:00:00"
      }
    ],
    "total": 10,
    "current": 1,
    "size": 10
  }
}
```

---

### 6.3 上传文档到知识库

**接口**: `POST /knowledge-bases/{kbId}/documents/upload`

**请求**: multipart/form-data

**表单参数**:
- `file`: 文件（支持 .txt, .md, .pdf, .docx）

**响应**:
```json
{
  "code": 200,
  "data": {
    "docId": 100,
    "fileName": "product_guide.pdf",
    "fileSize": 1024000,
    "message": "文件上传成功，请调用批量处理接口进行向量化"
  }
}
```

---

### 6.4 批量处理知识库文档（向量化）

**接口**: `POST /rag/batch-embed`

**请求参数**:
```json
{
  "kbId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "批量处理完成，成功: 15/20"
}
```

**特性**:
- 📄 自动从文件系统读取文档内容
- 🔢 智能分块（默认500字符，重叠50字符）
- 🧬 生成向量嵌入并存储到VectorStore
- 📊 支持绝对路径和相对路径

---

### 6.5 检索知识库

**接口**: `POST /rag/retrieve`

**请求参数**:
```json
{
  "kbIds": [1, 2],
  "query": "如何安装产品？",
  "topK": 5,
  "similarityThreshold": 0.7
}
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "documentId": 100,
      "title": "产品安装指南",
      "content": "安装步骤如下：1. 下载安装包...",
      "similarityScore": 0.92,
      "metadata": {
        "fileName": "installation_guide.md",
        "chunkIndex": 1
      }
    }
  ]
}
```

---

### 6.6 获取知识库文档列表

**接口**: `GET /knowledge-bases/{kbId}/documents?page={page}&pageSize={size}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 100,
        "fileName": "product_guide.pdf",
        "fileSize": 1024000,
        "status": "PROCESSED",
        "chunkCount": 25,
        "createdAt": "2026-04-01T10:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10
  }
}
```

---

### 6.7 删除文档

**接口**: `DELETE /knowledge-bases/{kbId}/documents/{docId}`

**响应**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 七、长期记忆模块

### 7.1 存储记忆

**接口**: `POST /memory/store`

**请求参数**:
```json
{
  "agentId": 100,
  "memoryType": "USER_PREFERENCE",
  "memoryValue": "用户偏好使用Python进行数据分析",
  "confidenceScore": 0.9,
  "tags": "python,data_analysis,preference",
  "metadataJson": "{\"source\":\"conversation\",\"topic\":\"programming\"}",
  "expiresAt": "2027-04-19T00:00:00"
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "memoryId": 789
  }
}
```

---

### 7.2 检索记忆

**接口**: `GET /memory/retrieve?agentId={id}&query={text}&memoryTypes={types}&limit={n}`

**查询参数**:
- `agentId`: 智能体ID（可选）
- `query`: 查询文本（可选，用于关键词匹配）
- `memoryTypes`: 记忆类型列表（可选：USER_PREFERENCE/PROJECT_CONTEXT/DECISION_LOGIC/INTERACTION_PATTERN）
- `limit`: 返回数量限制（默认5）

**注意**: userId从Session中自动获取，无需在URL中传递

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 789,
      "userId": 1,
      "memoryType": "USER_PREFERENCE",
      "memoryValue": "用户偏好使用Python进行数据分析",
      "confidenceScore": 0.9,
      "usageCount": 5,
      "tags": "python,data_analysis,preference",
      "lastUsedAt": "2026-04-18T10:00:00"
    }
  ]
}
```

---

### 7.3 向量相似度检索

**接口**: `POST /memory/retrieve-by-similarity`

**请求参数**:
```json
{
  "agentId": 100,
  "embedding": [0.1, 0.2, 0.3, ...],
  "similarityThreshold": 0.7,
  "limit": 5
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**: 同 7.2 retrieve接口，按相似度排序

---

### 7.4 更新记忆

**接口**: `PUT /memory/{memoryId}`

**请求参数**:
```json
{
  "content": "更新后的记忆内容",
  "metadata": {
    "updatedBy": "system",
    "reason": "user_feedback"
  }
}
```

**响应**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 7.5 删除记忆（软删除）

**接口**: `DELETE /memory/{memoryId}`

**响应**:
```json
{
  "code": 200,
  "message": "记忆删除成功"
}
```

---

### 7.6 记录记忆访问

**接口**: `POST /memory/{memoryId}/access`

**用途**: 更新记忆的usageCount和lastUsedAt，用于热度排序

**响应**:
```json
{
  "code": 200,
  "message": "访问记录成功"
}
```

---

### 7.7 提取并存储对话记忆

**接口**: `POST /memory/extract-from-conversation`

**请求参数**:
```json
{
  "agentId": 100,
  "conversationContent": "用户: 我喜欢用Python\n助手: 好的...",
  "decisionPath": ["step_1", "step_2"]
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "extractedMemories": 3,
    "storedMemories": [789, 790, 791]
  }
}
```

**特性**:
- 🧠 LLM自动提取关键记忆
- 📊 分类存储（偏好/项目/决策/交互）
- 🎯 计算置信度评分

---

### 7.8 获取用户画像

**接口**: `GET /memory/user-profile`

**注意**: userId从Session中自动获取，无需在URL中传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "preferences": [
      {
        "content": "用户偏好使用Python",
        "confidence": 0.9,
        "usageCount": 5
      }
    ],
    "projectContexts": [...],
    "decisionLogics": [...],
    "interactionPatterns": [...],
    "totalMemories": 25
  }
}
```

---

## 八、技能市场模块

### 8.1 发布技能包

**接口**: `POST /skill-market/publish`

**请求参数**:
```json
{
  "skillName": "数据分析助手",
  "skillCode": "data_analysis_assistant",
  "description": "提供专业的数据分析服务",
  "category": "data_analysis",
  "version": "1.0.0",
  "skillConfigJson": "{\"executionType\":\"workflow\",\"steps\":[...]}",
  "inputSchemaJson": "{...}",
  "outputSchemaJson": "{...}",
  "exampleUsage": "如何使用此技能",
  "iconUrl": "https://example.com/icon.png",
  "isPublic": true
}
```

**注意**: authorId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "skillId": 100
  }
}
```

---

### 8.2 搜索技能

**接口**: `GET /skill-market/search?keyword={text}&category={cat}&page={n}&pageSize={n}`

**查询参数**:
- `keyword`: 搜索关键词（可选）
- `category`: 技能分类（可选：data_analysis/content_generation/automation/integration）
- `page`: 页码（默认1）
- `pageSize`: 每页数量（默认10）

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 100,
        "skillName": "数据分析助手",
        "skillCode": "data_analysis_assistant",
        "category": "data_analysis",
        "ratingAvg": 4.5,
        "downloadCount": 150,
        "authorName": "开发者A"
      }
    ],
    "total": 30,
    "current": 1,
    "size": 10
  }
}
```

---

### 8.3 获取技能详情

**接口**: `GET /skill-market/{skillId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 100,
    "skillName": "数据分析助手",
    "skillCode": "data_analysis_assistant",
    "description": "提供专业的数据分析服务",
    "category": "data_analysis",
    "version": "1.0.0",
    "authorId": 1,
    "authorName": "开发者A",
    "skillConfigJson": "{...}",
    "inputSchemaJson": "{...}",
    "outputSchemaJson": "{...}",
    "exampleUsage": "如何使用此技能",
    "iconUrl": "https://example.com/icon.png",
    "ratingAvg": 4.5,
    "ratingCount": 30,
    "downloadCount": 150,
    "isPublic": true,
    "createdAt": "2026-03-01T10:00:00"
  }
}
```

---

### 8.4 安装技能

**接口**: `POST /skill-market/install`

**请求参数**:
```json
{
  "agentId": 100,
  "skillId": 100,
  "config": {
    "customParam": "value"
  }
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "installationId": 200
  }
}
```

---

### 8.5 卸载技能

**接口**: `DELETE /skill-market/uninstall`

**请求参数**:
```json
{
  "agentId": 100,
  "installationId": 200
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "message": "卸载成功"
}
```

---

### 8.6 获取用户已安装技能

**接口**: `GET /skill-market/user-installed?agentId={id}`

**注意**: userId从Session中自动获取，无需在URL中传递

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "installationId": 200,
      "userId": 1,
      "skillId": 100,
      "skillName": "数据分析助手",
      "skillCode": "data_analysis_assistant",
      "category": "data_analysis",
      "status": 1,
      "installedAt": "2026-04-15T10:00:00"
    }
  ]
}
```

---

### 8.7 启用/禁用技能

**接口**: `PUT /skill-market/toggle`

**请求参数**:
```json
{
  "installationId": 200,
  "enabled": true
}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

---

### 8.8 执行技能

**接口**: `POST /skill-market/execute`

**请求参数**:
```json
{
  "installationId": 200,
  "inputParams": {
    "dataset": "sales_2024.csv",
    "analysisType": "trend"
  }
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "skillId": 100,
    "skillName": "数据分析助手",
    "executionType": "workflow",
    "output": {...},
    "message": "技能执行成功"
  }
}
```

**支持的执行类型**:
- `workflow`: 工作流类型，执行预定义步骤
- `tool_chain`: 工具链类型，按顺序调用多个工具
- `template`: 模板类型，使用模板生成内容
- `simple`: 简单类型，直接返回配置结果

---

### 8.9 评分技能

**接口**: `POST /skill-market/rate`

**请求参数**:
```json
{
  "skillId": 100,
  "rating": 4.5,
  "comment": "非常好用的技能"
}
```

**注意**: userId从Session中自动获取，无需前端传递

**响应**:
```json
{
  "code": 200,
  "message": "评分成功"
}
```

---

### 8.10 获取热门技能

**接口**: `GET /skill-market/popular?category={cat}&limit={n}`

**查询参数**:
- `category`: 技能分类（可选）
- `limit`: 返回数量限制（默认10）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 100,
      "skillName": "数据分析助手",
      "downloadCount": 150,
      "ratingAvg": 4.5
    }
  ]
}
```

---

### 8.11 更新技能包

**接口**: `PUT /skill-market/{skillId}`

**请求参数**: 同 8.1 发布接口（不包含下载次数、评分等字段）

**响应**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 8.12 删除技能包

**接口**: `DELETE /skill-market/{skillId}`

**注意**: userId从Session中自动获取，用于验证是否为作者本人

**响应**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 九、反馈与分析模块

### 9.1 提交用户反馈

**接口**: `POST /feedback/submit`

**请求参数**:
```json
{
  "logId": 1000,
  "rating": 4,
  "feedback": "回答很准确，但希望能更详细一些"
}
```

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

---

### 9.2 获取我的反馈统计

**接口**: `GET /feedback/my-statistics`

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalFeedbacks": 50,
    "averageRating": 4.2,
    "ratingDistribution": {
      "5": 20,
      "4": 15,
      "3": 10,
      "2": 3,
      "1": 2
    }
  }
}
```

---

### 9.3 获取智能体反馈统计

**接口**: `GET /feedback/agent/{agentId}/statistics`

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalFeedbacks": 150,
    "averageRating": 4.5,
    "ratingDistribution": {...}
  }
}
```

---

### 9.4 获取反馈趋势

**接口**: `GET /feedback/my-trend?days={n}`

**查询参数**:
- `days`: 天数（默认7）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "date": "2026-04-13",
      "averageRating": 4.0,
      "feedbackCount": 5
    },
    {
      "date": "2026-04-14",
      "averageRating": 4.5,
      "feedbackCount": 8
    }
  ]
}
```

---

### 9.5 获取低分反馈列表

**接口**: `GET /feedback/agent/{agentId}/low-ratings?limit={n}`

**查询参数**:
- `limit`: 返回数量限制（默认20）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "logId": 1000,
      "rating": 2,
      "feedback": "回答不够准确",
      "userQuery": "什么是机器学习？",
      "createdAt": "2026-04-18T10:00:00"
    }
  ]
}
```

---

### 9.6 获取反馈详情

**接口**: `GET /feedback/{logId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "logId": 1000,
    "userId": 1,
    "agentId": 100,
    "userQuery": "什么是机器学习？",
    "finalResponse": "机器学习是...",
    "rating": 4,
    "feedback": "回答很准确",
    "intentType": "QUESTION",
    "status": "COMPLETED",
    "errorMessage": null,
    "createdAt": "2026-04-18T10:00:00"
  }
}
```

---

### 9.7 获取智能体反馈分析报告

**接口**: `GET /feedback/agent/{agentId}/analysis/report`

**响应**:
```json
{
  "code": 200,
  "data": {
    "averageRating": 4.5,
    "totalFeedbacks": 150,
    "mainIssue": "部分回答过于简略",
    "problemCategories": {
      "accuracy": 10,
      "completeness": 15,
      "clarity": 5
    },
    "suggestions": [
      "增加示例说明",
      "提供更详细的解释"
    ]
  }
}
```

---

### 9.8 获取智能体反馈关键词

**接口**: `GET /feedback/agent/{agentId}/analysis/keywords`

**响应**:
```json
{
  "code": 200,
  "data": {
    "positiveKeywords": [
      {"keyword": "准确", "count": 30},
      {"keyword": "专业", "count": 25}
    ],
    "negativeKeywords": [
      {"keyword": "简略", "count": 10},
      {"keyword": "不清楚", "count": 5}
    ]
  }
}
```

---

### 9.9 获取智能体优化建议

**接口**: `GET /feedback/agent/{agentId}/analysis/suggestions`

**响应**:
```json
{
  "code": 200,
  "data": {
    "suggestions": [
      {
        "category": "completeness",
        "priority": "HIGH",
        "description": "增加示例说明，提高回答完整性",
        "affectedQueries": 15
      },
      {
        "category": "clarity",
        "priority": "MEDIUM",
        "description": "简化复杂概念的解释",
        "affectedQueries": 8
      }
    ]
  }
}
```

---

## 十、系统管理模块

### 10.1 获取系统状态

**接口**: `GET /system/status`

**响应**:
```json
{
  "code": 200,
  "data": {
    "uptime": "7 days 3 hours",
    "activeUsers": 150,
    "totalAgents": 50,
    "totalWorkflows": 30,
    "totalTools": 25,
    "systemHealth": "HEALTHY"
  }
}
```

---

### 10.2 获取系统统计信息

**接口**: `GET /system/statistics`

**响应**:
```json
{
  "code": 200,
  "data": {
    "todayRequests": 5000,
    "todayActiveUsers": 200,
    "totalExecutions": 50000,
    "averageResponseTime": 1500,
    "successRate": 98.5
  }
}
```

---

### 10.3 获取用户列表（管理员）

**接口**: `GET /admin/users?page={page}&pageSize={size}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userName": "管理员",
        "userRole": "ADMIN",
        "email": "admin@example.com",
        "createdAt": "2026-01-01T00:00:00"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

---

### 10.4 更新用户角色（管理员）

**接口**: `PUT /admin/users/{userId}/role`

**请求参数**:
```json
{
  "userRole": "ADMIN"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "角色更新成功"
}
```

---

### 10.5 禁用/启用用户（管理员）

**接口**: `PUT /admin/users/{userId}/toggle`

**请求参数**:
```json
{
  "enabled": false
}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

---

### 10.6 获取AI模型列表

**接口**: `GET /ai-models?page={page}&pageSize={size}&modelType={type}`

**查询参数**:
- `page`: 页码（默认1）
- `pageSize`: 每页数量（默认10）
- `modelType`: 模型类型（可选：CHAT/EMBEDDING/IMAGE）

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "modelName": "qwen2.5:7b",
        "providerName": "Ollama",
        "modelType": "CHAT",
        "isActive": true,
        "maxTokens": 8192
      }
    ],
    "total": 10,
    "current": 1,
    "size": 10
  }
}
```

---

### 10.7 创建AI模型

**接口**: `POST /ai-models`

**请求参数**:
```json
{
  "providerId": 1,
  "modelName": "gpt-4",
  "modelType": "CHAT",
  "displayName": "GPT-4",
  "description": "OpenAI GPT-4模型",
  "maxTokens": 8192,
  "isActive": true
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "modelId": 5
  }
}
```

---

### 10.8 更新AI模型

**接口**: `PUT /ai-models/{modelId}`

**请求参数**: 同 10.7 创建接口

**响应**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 10.9 删除AI模型

**接口**: `DELETE /ai-models/{modelId}`

**响应**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 10.10 获取模型提供商列表

**接口**: `GET /model-providers`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "providerCode": "ollama",
      "providerName": "Ollama本地",
      "baseUrl": "http://localhost:11434",
      "isActive": true
    },
    {
      "id": 2,
      "providerCode": "openai",
      "providerName": "OpenAI",
      "baseUrl": "https://api.openai.com/v1",
      "isActive": true
    }
  ]
}
```

---

## 附录

### A. 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token失效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

### B. 通用响应格式

**成功响应**:
```json
{
  "code": 200,
  "data": {...},
  "message": "成功"
}
```

**失败响应**:
```json
{
  "code": 400,
  "data": null,
  "message": "错误描述"
}
```

---

### C. 分页响应格式

```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

---

### D. 认证说明

**重要**: userId严禁由前端传递，必须从Session中获取

**正确做法**:
```javascript
// ✅ 正确：只需确保已登录，Session会自动携带
fetch('/api/v1/unified-agent/execute', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',  // 自动携带Cookie（包含Session ID）
  body: JSON.stringify({ 
    agentId: 123, 
    query: "帮我分析数据" 
  })
});
```

**错误做法**:
```javascript
// ❌ 错误：不要手动传递userId
fetch('/api/v1/unified-agent/execute', {
  method: 'POST',
  body: JSON.stringify({ 
    agentId: 123, 
    userId: 456,  // ❌ 禁止！
    query: "帮我分析数据" 
  })
});
```

---

### E. SSE事件类型说明

| 事件名 | 说明 | 数据格式 |
|--------|------|----------|
| planning | 任务规划中 | `{message, progress}` |
| plan_ready | 任务规划完成 | `{taskId, totalSteps, steps, progress}` |
| step_start | 步骤开始执行 | `{stepNumber, totalSteps, description, progress}` |
| step_complete | 步骤执行完成 | `{stepNumber, result, progress}` |
| generating_response | 生成最终回答 | `{message, progress}` |
| complete | 执行完成 | `{success, finalResponse, executionTimeMs, progress}` |
| error | 发生错误 | `{message, error}` |

---

### F. 记忆类型说明

| 类型 | 说明 | 示例 |
|------|------|------|
| USER_PREFERENCE | 用户偏好 | "用户喜欢Python" |
| PROJECT_CONTEXT | 项目背景 | "正在进行电商项目" |
| DECISION_LOGIC | 决策逻辑 | "优先选择开源方案" |
| INTERACTION_PATTERN | 交互模式 | "用户喜欢简洁回答" |

---

### G. 技能分类说明

| 分类 | 说明 |
|------|------|
| data_analysis | 数据分析类技能 |
| content_generation | 内容生成类技能 |
| automation | 自动化类技能 |
| integration | 集成类技能 |

---

### H. 工具来源类型说明

| 类型 | 说明 |
|------|------|
| SYSTEM | 系统内置工具 |
| USER_HTTP | 用户自定义HTTP工具 |
| USER_MCP | 用户MCP协议工具 |
| USER_AGENT | 用户智能体作为工具 |

---

### I. 工作流模式说明

| 模式 | 说明 |
|------|------|
| FULL_AUTO | 全自动：AI完全生成工作流 |
| SEMI_CUSTOM | 半定制：用户提供部分节点，AI补充 |
| FULL_CUSTOM | 完全定制：用户手动创建所有节点 |

---

### J. 健康状态说明

| 状态码 | 说明 |
|--------|------|
| 0 | 未知 |
| 1 | 健康 |
| 2 | 异常 |
| 3 | 禁用 |

---

### K. SSE流式接口使用说明

**SSE接口**: `GET /api/unified-agent/execute-stream`

**查询参数**:
- `agentId`: 智能体ID（必填）
- `query`: 用户查询（必填，需要URL编码）
- `workflowId`: 工作流ID（可选）
- `sessionId`: 会话ID（可选，用于多轮对话关联。首次对话不传，后续对话使用返回的sessionId）
- `token`: Session Token（可选，用于备用认证）

**注意**: 
1. userId从Session中自动获取，无需在URL中传递
2. **重要**: 前端必须设置 `withCredentials: true` 以携带Cookie
3. **会话管理**: 首次对话时不传sessionId，后端会生成并返回；后续对话传入该sessionId实现多轮对话关联

**响应类型**: `text/event-stream`

#### 前端调用示例

**方法1: 使用原生 EventSource（推荐）**
```javascript
// 会话管理：存储当前会话ID
let currentSessionId = null;

function startChat(agentId, query) {
  const url = `/api/unified-agent/execute-stream?agentId=${agentId}&query=${encodeURIComponent(query)}${currentSessionId ? `&sessionId=${currentSessionId}` : ''}`;

  const eventSource = new EventSource(url, {
    withCredentials: true  // 重要：携带Cookie
  });

  // ✅ 监听规划事件 - 显示待办清单
  eventSource.addEventListener('plan_ready', (event) => {
    const data = JSON.parse(event.data);
    
    // 保存sessionId
    if (!currentSessionId && data.sessionId) {
      currentSessionId = data.sessionId;
    }
    
    // ✅ 在输入框上方显示待办清单抽屉
    showTodoDrawer(data.steps);
  });

  // ✅ 监听步骤开始 - 标记为执行中（蓝色）
  eventSource.addEventListener('step_start', (event) => {
    const data = JSON.parse(event.data);
    
    // ✅ 更新待办清单：将当前步骤标记为“执行中”
    updateStepStatus(data.stepId, 'running');
  });

  // ✅ 监听步骤完成 - 标记为成功（绿色）或失败（红色）
  eventSource.addEventListener('step_complete', (event) => {
    const data = JSON.parse(event.data);
    
    // ✅ 更新待办清单：根据status标记颜色
    updateStepStatus(data.stepId, data.status);  // 'success' 或 'failed'
  });

  // ✅ 监听生成响应事件 - 显示“AI正在输入”
  eventSource.addEventListener('generating_response', (event) => {
    const data = JSON.parse(event.data);
    showTypingIndicator();  // 显示打字动画
  });

  // ✅ 监听完成事件 - 只显示AI的最终回复
  eventSource.addEventListener('complete', (event) => {
    const data = JSON.parse(event.data);
    
    // ✅ 隐藏待办清单抽屉
    hideTodoDrawer();
    
    // ✅ 将AI的完整回复添加到一个气泡中
    addMessageToChat({
      role: 'assistant',
      content: data.finalResponse,  // AI的完整回复（Markdown格式）
      sessionId: data.sessionId,
      timestamp: new Date().toISOString()
    });
    
    // 关闭连接
    eventSource.close();
  });

  // 监听错误事件
  eventSource.addEventListener('error', (event) => {
    console.error('SSE错误:', event.data);
    hideTodoDrawer();
    eventSource.close();
  });

  eventSource.onerror = (error) => {
    console.error('SSE连接错误:', error);
    hideTodoDrawer();
    eventSource.close();
  };
}

/**
 * 显示待办清单抽屉（输入框上方）
 */
function showTodoDrawer(steps) {
  const drawer = document.getElementById('todo-drawer');
  drawer.innerHTML = '';
  drawer.style.display = 'block';
  
  steps.forEach((step, index) => {
    const stepItem = document.createElement('div');
    stepItem.className = 'todo-item pending';  // 默认白色（待执行）
    stepItem.id = `todo-${step.stepId}`;
    
    stepItem.innerHTML = `
      <span class="todo-icon">⚪</span>
      <span class="todo-text">${index + 1}. ${step.description}</span>
    `;
    
    drawer.appendChild(stepItem);
  });
}

/**
 * 更新步骤状态
 * @param {string} stepId - 步骤ID
 * @param {string} status - 状态：'running' | 'success' | 'failed'
 */
function updateStepStatus(stepId, status) {
  const stepElement = document.getElementById(`todo-${stepId}`);
  if (!stepElement) return;
  
  // 移除所有状态类
  stepElement.classList.remove('pending', 'running', 'success', 'failed');
  
  // 添加新状态
  stepElement.classList.add(status);
  
  // 更新图标
  const icon = stepElement.querySelector('.todo-icon');
  switch (status) {
    case 'running':
      icon.textContent = '🔵';  // 蓝色 - 执行中
      break;
    case 'success':
      icon.textContent = '✅';  // 绿色 - 成功
      break;
    case 'failed':
      icon.textContent = '❌';  // 红色 - 失败
      break;
    default:
      icon.textContent = '⚪';  // 白色 - 待执行
  }
}

/**
 * 隐藏待办清单抽屉
 */
function hideTodoDrawer() {
  const drawer = document.getElementById('todo-drawer');
  drawer.style.display = 'none';
}

/**
 * 添加消息到聊天界面
 */
function addMessageToChat(message) {
  const chatContainer = document.getElementById('chat-messages');
  
  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${message.role}`;
  
  // 如果是AI回复，渲染Markdown
  if (message.role === 'assistant') {
    messageDiv.innerHTML = marked.parse(message.content);  // 使用marked.js
  } else {
    messageDiv.textContent = message.content;
  }
  
  chatContainer.appendChild(messageDiv);
  chatContainer.scrollTop = chatContainer.scrollHeight;
}

/**
 * 显示打字指示器
 */
function showTypingIndicator() {
  const indicator = document.getElementById('typing-indicator');
  indicator.style.display = 'block';
}
```

**HTML结构示例**:
```html
<!-- 待办清单抽屉（输入框上方） -->
<div id="todo-drawer" class="todo-drawer" style="display: none;">
  <!-- 动态生成的待办项 -->
</div>

<!-- 聊天消息区域 -->
<div id="chat-messages" class="chat-messages">
  <!-- 对话气泡 -->
</div>

<!-- 打字指示器 -->
<div id="typing-indicator" class="typing-indicator" style="display: none;">
  <span></span><span></span><span></span>
</div>

<!-- 输入框 -->
<div class="input-area">
  <input type="text" id="user-input" placeholder="输入消息..." />
  <button onclick="sendMessage()">发送</button>
</div>
```

**CSS样式示例**:
```css
/* 待办清单抽屉 */
.todo-drawer {
  position: fixed;
  bottom: 80px;  /* 输入框上方 */
  left: 50%;
  transform: translateX(-50%);
  width: 600px;
  max-height: 300px;
  overflow-y: auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 16px;
  z-index: 1000;
}

/* 待办项 */
.todo-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin: 4px 0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.todo-icon {
  font-size: 18px;
  margin-right: 8px;
  min-width: 24px;
}

.todo-text {
  font-size: 14px;
  color: #333;
}

/* 状态样式 */
.todo-item.pending {
  background-color: #f5f5f5;  /* ⚪ 白色 - 待执行 */
  color: #999;
}

.todo-item.running {
  background-color: #e6f7ff;  /* 🔵 蓝色 - 执行中 */
  color: #1890ff;
  animation: pulse 1.5s infinite;
}

.todo-item.success {
  background-color: #f6ffed;  /* ✅ 绿色 - 成功 */
  color: #52c41a;
}

.todo-item.failed {
  background-color: #fff2f0;  /* ❌ 红色 - 失败 */
  color: #ff4d4f;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 消息气泡 */
.message {
  max-width: 80%;
  margin: 10px 0;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}

.message.user {
  background-color: #007bff;
  color: white;
  margin-left: auto;
  border-bottom-right-radius: 4px;
}

.message.assistant {
  background-color: #f0f0f0;
  color: #333;
  margin-right: auto;
  border-bottom-left-radius: 4px;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f0f0f0;
  border-radius: 12px;
  width: fit-content;
  margin: 10px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}
```

**方法2: 使用 fetch + ReadableStream**
```javascript
async function executeStream(agentId, query, workflowId = null) {
  const url = `/api/unified-agent/execute-stream?agentId=${agentId}&query=${encodeURIComponent(query)}${workflowId ? `&workflowId=${workflowId}` : ''}`;
  
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',  // 重要：携带Cookie
    headers: {
      'Accept': 'text/event-stream'
    }
  });
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop(); // 保留未完成的行
    
    for (const line of lines) {
      if (line.startsWith('event:')) {
        const eventType = line.slice(6).trim();
        // 读取下一行的data
        const nextLine = lines.shift();
        if (nextLine && nextLine.startsWith('data:')) {
          const data = JSON.parse(nextLine.slice(5).trim());
          handleSSEEvent(eventType, data);
        }
      }
    }
  }
}

function handleSSEEvent(eventType, data) {
  switch (eventType) {
    case 'planning':
      console.log('规划中:', data.message);
      break;
    case 'complete':
      console.log('完成:', data.finalResponse);
      break;
    case 'error':
      console.error('错误:', data);
      break;
  }
}
```

#### 常见问题

**Q1: SSE连接立即断开，提示"未登录"？**

A: 这是因为浏览器没有携带Session Cookie。解决方法：

1. **确保前端设置了 `withCredentials: true`**
   ```javascript
   // EventSource
   new EventSource(url, { withCredentials: true });
   
   // fetch
   fetch(url, { credentials: 'include' });
   ```

2. **检查后端CORS配置**
   - `Access-Control-Allow-Credentials: true`
   - `Access-Control-Allow-Origin` 不能使用 `*`，必须指定具体域名

3. **确认用户已登录**
   - 先调用登录接口获取Session
   - 浏览器会自动存储JSESSIONID Cookie

4. **检查Cookie是否被阻止**
   - 打开浏览器开发者工具 → Network
   - 查看请求头中是否有 `Cookie: JSESSIONID=xxx`
   - 如果没有，检查浏览器的Cookie设置

**Q2: 跨域时SSE无法携带Cookie？**

A: 需要满足以下条件：
1. 后端CORS配置正确（已配置）
2. 前端设置 `withCredentials: true`
3. 前后端域名都在同一顶级域名下（如 `api.example.com` 和 `www.example.com`）
4. 或者使用代理服务器转发请求

**Q3: SSE连接超时？**

A: 默认超时时间为5分钟（300秒）。如果任务执行时间较长：
- 后端可以调整 `SseEmitter` 的超时时间
- 前端可以实现重连机制

**Q4: 如何显示每个步骤的AI回复？**

A: 在 `step_complete` 事件中，如果该步骤有AI推理，会返回 `aiResponse` 字段。前端应该：

1. **更新待办清单状态** - 根据 `status` 字段变色
2. **显示AI回复气泡** - 如果有 `aiResponse`，立即添加到聊天区域

```javascript
eventSource.addEventListener('step_complete', (event) => {
  const data = JSON.parse(event.data);
  
  // 1. 更新待办清单（白/蓝/绿/红）
  updateTodoStatus(data.stepId, data.status);
  
  // 2. 如果有AI回复，显示为气泡
  if (data.aiResponse) {
    addMessageToChat({
      role: 'assistant',
      content: `### ${data.description}\n\n${data.aiResponse}`,
      sessionId: data.sessionId,
      isStepResponse: true
    });
  }
});
```

---

**文档版本**: v2.0  
**最后更新**: 2026-04-19  
**维护者**: Agent Mesh开发团队  
**联系方式**: support@agentmesh.com
