# 医疗领域智能体创建指南

## 概述

本指南将帮助你使用 Agent Mesh 平台创建一个具有现实意义的医疗健康领域智能体。我们将创建一个"医疗健康助手"，它能够基于专业的医疗知识库回答用户关于健康、疾病和治疗的问题。

## 前置条件

1. 确保已安装并运行 Agent Mesh 后端服务
2. 确保数据库中存在有效的模型提供商和模型配置
3. 确保向量数据库（如 Ollama）已正确配置

## 步骤一：创建医疗知识库

### 1. 执行数据库初始化脚本

运行以下 SQL 脚本来创建医疗知识库：

```bash
psql -U agent_mesh -d agent_mesh -f sql/medical_knowledge_base_init.sql
```

或者在你的数据库管理工具中执行 `sql/medical_knowledge_base_init.sql` 文件中的内容。

### 2. 通过 API 创建知识库（可选）

如果你更喜欢通过 API 创建，可以使用以下请求：

```http
POST /knowledge-base/add
Content-Type: application/json

{
  "name": "医疗健康知识库",
  "description": "包含常见疾病、症状、治疗方案等医疗信息的知识库",
  "vectorStoreType": "OLLAMA",
  "vectorStoreTable": "ollama_vector_store",
  "embeddingModelId": 3,
  "chunkSize": 500,
  "chunkOverlap": 50,
  "status": 1,
  "visibility": 2
}
```

## 步骤二：添加医疗文档到知识库

### 1. 准备医疗文档内容

你可以准备以下类型的医疗文档：

- **常见疾病诊疗指南**：包含常见疾病的症状、诊断标准、治疗原则等
- **常用药物说明书**：药物的适应症、用法用量、不良反应等信息
- **健康生活指南**：饮食建议、运动指导、预防措施等

### 2. 通过 API 添加文档

```http
POST /knowledge-base/document/add/{kbId}
Content-Type: application/json

{
  "docName": "常见疾病诊疗指南",
  "docType": "TEXT",
  "metadataJson": {
    "author": "医疗专家组",
    "category": "疾病诊疗",
    "version": "1.0"
  }
}
```

### 3. 批量导入文档（推荐）

```http
POST /knowledge-base/document/batch-import/{kbId}
Content-Type: application/json

{
  "documents": [
    {
      "docName": "常见疾病诊疗指南",
      "docType": "TEXT",
      "content": "这里是文档内容...",
      "metadataJson": {
        "author": "医疗专家组",
        "category": "疾病诊疗"
      }
    },
    {
      "docName": "常用药物说明书",
      "docType": "TEXT",
      "content": "这里是文档内容...",
      "metadataJson": {
        "author": "药学部",
        "category": "药物信息"
      }
    }
  ]
}
```

## 步骤三：创建医疗助手智能体

### 1. 执行智能体初始化脚本

运行以下 SQL 脚本来创建医疗助手智能体：

```bash
psql -U agent_mesh -d agent_mesh -f sql/medical_agent_init.sql
```

### 2. 通过 API 创建智能体（可选）

```http
POST /agent/add
Content-Type: application/json

{
  "name": "医疗健康助手",
  "description": "专业的医疗健康咨询助手，能够回答常见疾病、症状、治疗方案等问题",
  "systemPrompt": "你是一个专业的医疗健康助手，具备丰富的医学知识。你的主要职责是：\n1. 回答用户关于常见疾病的疑问\n2. 提供健康生活方式建议\n3. 解释医学术语和检查结果\n4. 给出一般性的健康指导\n\n重要原则：\n- 始终强调你提供的信息仅供参考，不能替代专业医生的诊断和治疗\n- 对于紧急或严重情况，建议用户立即就医\n- 使用通俗易懂的语言解释医学概念\n- 保持专业、准确、负责任的态度",
  "roleDefinition": "医疗健康领域的专业顾问，具有内科、全科医学背景，擅长健康咨询和疾病预防指导",
  "decisionModelId": 1,
  "responseModelId": 2,
  "isToolEnabled": false,
  "version": "1.0.0",
  "status": 1,
  "visibility": 2,
  "modelSelectionStrategy": "ADAPTIVE",
  "budgetConstraint": 0.1
}
```

## 步骤四：关联智能体与知识库

### 1. 通过 API 建立关联

```http
POST /api/agent-network/kb-relation/add
Content-Type: application/json

{
  "agentId": 1,
  "kbId": 1,
  "searchTopK": 5,
  "similarityThreshold": 0.7,
  "sortOrder": 1
}
```

## 步骤五：测试智能体

### 1. 发送测试请求

```http
POST /unified-agent/execute
Content-Type: application/json

{
  "agentId": 1,
  "query": "我最近经常头痛，可能是什么原因？",
  "userId": 1
}
```

### 2. 示例对话

**用户**: "我最近经常头痛，可能是什么原因？"

**医疗助手**: "头痛可能由多种原因引起，常见的包括：

1. **紧张性头痛**：最常见类型，通常与压力、焦虑或肌肉紧张有关
2. **偏头痛**：常伴有恶心、对光敏感等症状
3. **鼻窦炎**：伴随鼻塞、面部压痛
4. **眼部问题**：如视力疲劳
5. **脱水或饥饿**

⚠️ **重要提醒**：
- 如果头痛突然剧烈发作、伴有发热、颈部僵硬、意识改变，请立即就医
- 如果头痛持续加重或频繁发作，建议咨询医生进行专业评估
- 以上信息仅供参考，不能替代专业医疗诊断

建议你记录头痛的时间、持续时间、强度和可能的诱因，这将有助于医生诊断。"

## 步骤六：优化和改进

### 1. 调整检索参数

根据实际效果调整知识库检索参数：

- `searchTopK`: 增加或减少返回的相关文档数量
- `similarityThreshold`: 调整相似度阈值以提高或降低检索严格度

### 2. 扩展知识库内容

持续添加更多医疗文档，如：

- 专科疾病指南（心血管、内分泌等）
- 儿童健康专题
- 老年人健康管理
- 慢性病管理指南

### 3. 启用工具功能（可选）

为智能体添加工具能力，如：

- 症状检查工具
- 药物相互作用查询
- 医院/医生推荐

## 注意事项

### 医疗免责声明

在系统提示词中必须包含明确的免责声明：

```
重要声明：本助手提供的信息仅供参考和教育目的，不能替代专业医疗建议、诊断或治疗。
如有健康问题，请咨询合格的医疗专业人员。
在紧急情况下，请立即拨打急救电话或前往最近的急诊室。
```

### 数据安全与隐私

- 不要存储用户的个人健康信息（PHI）
- 确保所有数据传输加密
- 遵守相关医疗数据保护法规（如 HIPAA、GDPR 等）

### 内容准确性

- 定期更新知识库内容
- 确保信息来源可靠（如权威医疗机构、同行评审期刊）
- 标注信息的来源和更新日期

## 其他领域示例

除了医疗领域，你还可以创建以下领域的智能体：

### 1. 法律咨询助手
- 知识库：法律法规、案例分析、法律程序
- 应用场景：合同审查、法律条款解释

### 2. 技术支持助手
- 知识库：产品文档、故障排除指南、API 文档
- 应用场景：软件使用帮助、技术问题解答

### 3. 教育培训助手
- 知识库：课程资料、教学大纲、习题库
- 应用场景：在线辅导、作业批改、学习建议

### 4. 金融理财助手
- 知识库：金融产品说明、投资策略、税务知识
- 应用场景：理财规划、投资建议、风险评估

## 故障排查

### 问题 1：智能体无法检索到知识库内容

**解决方案**：
1. 检查 `agent_kb_relation` 表中的关联是否正确
2. 确认知识库状态为启用（status = 1）
3. 验证文档是否已完成向量化处理（status = 1）

### 问题 2：检索结果不相关

**解决方案**：
1. 降低 `similarityThreshold` 值（如从 0.7 降到 0.5）
2. 增加 `searchTopK` 值以获取更多候选文档
3. 优化文档内容的质量和相关性

### 问题 3：智能体响应不准确

**解决方案**：
1. 优化 `systemPrompt`，提供更明确的指导
2. 添加更多高质量的知识库文档
3. 调整决策模型和回复模型的配置

## 总结

通过以上步骤，你已经成功创建了一个医疗健康领域的智能体。这个智能体可以：

✅ 基于专业知识库回答医疗相关问题  
✅ 提供健康生活方式建议  
✅ 解释医学术语和概念  
✅ 引导用户在必要时寻求专业医疗帮助  

继续扩展和优化你的智能体，使其能够更好地服务于特定领域的需求！