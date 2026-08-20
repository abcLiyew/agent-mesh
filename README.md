# agent-mesh

基于 Spring Boot 3 + Spring AI 的 **AI 自主决策引擎**（后端服务）。将智能体注册为可复用工具，由轻量模型自动规划调用链路，实现无预设流程的智能问答与任务执行，并提供 OpenAI 兼容 API 供任意语言集成。

## 核心能力

- **AI 自主决策**：轻量模型识别用户意图，自动生成工具调用链路
- **智能体即工具**：已创建的智能体可被其他智能体复用调用，避免重复开发
- **多模型协同**：小模型决策 + 大模型作答，在成本与质量间取得平衡
- **RAG 知识库**：PDF / Word / TXT 文档解析、向量化与 pgvector 语义检索
- **MCP 服务**：接入阿里云 MCP 与自定义 MCP 服务，扩展外部能力
- **统一工作流引擎**：长期记忆、技能市场，支持自主 / 指定 / 动态混合执行
- **OpenAI 兼容 API**：`/v1/chat/completions`，支持 `tool_calls`、SSE 流式、多轮对话
- **成本监控**：Token 计数与多模型成本统计，辅助模型选型

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 后端框架 | Spring Boot 3.2 + Spring AI 1.1 + spring-ai-alibaba |
| AI 模型 | Qwen（DashScope）/ Ollama / OpenAI 兼容 |
| 向量数据库 | pgvector（PostgreSQL） |
| 存储 | PostgreSQL + MyBatis-Plus |
| 接口文档 | Knife4j（OpenAPI 3） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL（需启用 pgvector 扩展）

### 启动后端

```bash
git clone https://github.com/abcLiyew/agent-mesh.git
cd agent-mesh

# 初始化数据库（创建表结构）
psql -U postgres -d agent_mesh -f sql/agent_mesh.sql

# 配置模型 API Key 后启动
mvn spring-boot:run
```

启动后访问 `http://localhost:8080/doc.html` 查看 Knife4j 接口文档。

### 调用示例

每个智能体都可通过标准 OpenAI 协议调用：

```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_api_key" \
  -d '{
    "model": "your-agent-id",
    "messages": [{"role": "user", "content": "查询订单 ORDER123 的状态"}],
    "stream": false
  }'
```

## 项目结构

```
src/main/java/com/esdllm/agentmesh/
├── controller/        # REST 接口层（统一引擎、MCP、工作流、长期记忆、技能市场等）
├── service/
│   ├── agent/         # 智能体核心：工具调用、智能体网络、依赖管理
│   ├── unified/       # 统一智能体引擎：长期记忆、技能市场、模型策略
│   ├── workflow/      # 工作流编排
│   ├── rag/           # RAG 知识库：文档处理、向量检索
│   ├── model/         # 多模型管理、成本监控、Token 计数
│   └── ...
├── model/             # 领域模型与 DTO
└── repository/        # MyBatis-Plus 数据访问层
```

## 文档

- [API 接口文档](API_DOCUMENTATION.md)
- [统一智能体引擎](UNIFIED_AGENT_ENGINE.md)
- [工作流使用指南](WORKFLOW_USAGE_GUIDE.md)
- [后端架构与核心算法技术报告](后端架构与核心算法技术报告.md)

## License

[MIT License](LICENSE)
