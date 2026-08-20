# 医疗领域智能体 - 快速开始

## 概述

本项目提供了一个完整的医疗健康领域智能体示例，包括：
- 医疗知识库（包含常见疾病、药物、健康生活方式等信息）
- 医疗助手智能体（专业的健康咨询助手）
- 知识库与智能体的自动关联
- 完整的使用文档和示例代码

## 快速开始

### 方式一：使用 SQL 脚本（推荐）

1. 执行数据库初始化脚本

```bash
psql -U agent_mesh -d agent_mesh -f sql/medical_knowledge_base_init.sql
psql -U agent_mesh -d agent_mesh -f sql/medical_agent_init.sql
```

2. 启动应用

```bash
mvn spring-boot:run
```

3. 测试智能体

```bash
curl -X POST http://localhost:8080/unified-agent/execute \
  -H "Content-Type: application/json" \
  -d '{"agentId": 1, "query": "我最近经常头痛，可能是什么原因？", "userId": 1}'
```

### 方式二：使用 API 一键创建

1. 启动应用

```bash
mvn spring-boot:run
```

2. 调用一键创建接口

```bash
curl -X POST http://localhost:8080/example/medical/create-system \
  -H "Content-Type: application/json" \
  --cookie "your-session-cookie"
```

## 使用示例

### 示例 1：症状咨询

请求：我最近经常头痛，可能是什么原因？

预期响应：医疗助手会提供头痛的可能原因、建议措施，并提醒用户在严重情况下就医。

### 示例 2：慢性病管理

请求：高血压患者应该注意什么？

预期响应：提供高血压患者的饮食建议、生活方式调整、定期监测等指导。

### 示例 3：健康指标解释

请求：什么是BMI指数，如何计算？

预期响应：解释BMI的定义、计算方法、正常范围等信息。

## 重要提醒

### 医疗免责声明

本示例仅用于技术演示和教育目的。在实际应用中：

1. 必须包含明确的免责声明
2. 不能替代专业医疗建议
3. 紧急情况应引导用户立即就医
4. 遵守相关医疗数据保护法规

## 故障排查

### 问题 1：智能体无法检索到知识库内容

检查清单：
- 确认 agent_kb_relation 表中有正确的关联记录
- 确认知识库状态为启用（status = 1）
- 确认文档已完成向量化处理（status = 1）

### 问题 2：检索结果不相关

解决方案：
1. 降低相似度阈值到 0.5
2. 增加返回结果数到 10

## 更多资源

- 详细使用指南：MEDICAL_AGENT_GUIDE.md
- 项目主文档：README.md
- API 文档：API_DOCUMENTATION.md

## 下一步

创建成功后，你可以：

1. 扩展知识库：添加更多医疗领域的专业文档
2. 优化智能体：调整系统提示词和模型配置
3. 添加新领域：参考此示例创建其他领域的智能体