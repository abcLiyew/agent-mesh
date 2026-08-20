# 流式 API 调用说明文档

## 概述

本项目使用 **Spring AI 1.1.0** 的 Flux API 实现流式响应，支持实时逐字输出模型生成内容。流式 API 基于 **Reactor** 框架的 `Flux` 类型，采用响应式编程范式。

---

## 核心组件

### 1. AiModelSupport - 流式调用封装

**位置**: `com.esdllm.agentmesh.service.agent.support.AiModelSupport`

**主要方法**:

```java
/**
 * 调用 LLM 生成回答（流式 Flux）
 * @param chatModel ChatModel 实例
 * @param query 用户问题
 * @param toolResponse 工具调用结果（可选）
 * @param agent 智能体配置
 * @return Flux<String> 流式文本片段
 */
public Flux<String> streamChatModel(ChatModel chatModel, String query, String toolResponse, Agent agent)
```


**返回类型**: `reactor.core.publisher.Flux<String>`

**特点**:

- 每个发射项是一个文本片段（通常是几个字符或一个词）
- 异步非阻塞
- 支持背压（backpressure）

---

### 2. DecisionExecutorImpl - 流式决策执行

**位置**: `com.esdllm.agentmesh.service.agent.impl.DecisionExecutorImpl`

**接口定义**:

```java
/**
 * 执行流式决策
 * @param query 用户问题
 * @param agentId 智能体 ID
 * @param userId 用户 ID
 * @param emitter SSE 发射器
 */
void executeStream(String query, Long agentId, Long userId, SseEmitter emitter);
```


**执行流程**:

```
1. 意图识别 (Intent Recognition)
   └─> 发送进度事件：正在识别用户意图
   
2. 工具匹配 (Tool Matching)
   └─> 发送进度事件：正在匹配可用工具
   └─> 发送工具列表事件
   
3. 工具执行 (Tool Execution) [可选]
   └─> 发送工具调用开始事件
   └─> 发送工具调用完成事件
   
4. 流式回答生成 (Streaming Response)
   └─> 调用 AiModelSupport.streamChatModel()
   └─> 订阅 Flux 并推送 SSE 事件
   └─> 发送完成事件
```


---

## 外部调用方式

### 方式一：SSE HTTP 接口（推荐）

**接口地址**: `GET /api/decision/chat-stream/{agentId}`

**请求参数**:

| 参数名    | 类型   | 必填 | 说明                      |
| --------- | ------ | ---- | ------------------------- |
| agentId   | Long   | 是   | 智能体 ID                 |
| query     | String | 是   | 用户问题                  |
| sessionId | String | 否   | 会话 ID（可选，用于追踪） |

**请求示例**:

```bash
curl -N "http://localhost:8080/api/decision/chat-stream/2034932406663692289?query=你好" \
  -H "Accept: text/event-stream"
```


**响应格式**: `text/event-stream` (SSE)

**事件类型**:

1. **stream-start**: 流式响应开始
2. **stream-progress**: 处理进度更新
3. **intent-recognized**: 意图识别完成
4. **tool-matched**: 工具匹配完成
5. **tool-call-started**: 工具调用开始
6. **tool-call-completed**: 工具调用完成
7. **stream-data**: 文本数据块（多次发送）
8. **stream-complete**: 流式响应完成
9. **stream-error**: 错误事件

**响应事件示例**:

```event
event: stream-start
data: {"type":"START","timestamp":1710924373310,"data":"正在处理您的请求..."}

event: stream-progress
data: {"type":"PROGRESS","step":2,"totalSteps":5,"message":"正在识别用户意图"}

event: intent-recognized
data: {"type":"INTENT","intentType":"CHAT","confidence":0.9,"durationMs":15}

event: stream-data
data: {"type":"CHUNK","content":"你"}

event: stream-data
data: {"type":"CHUNK","content":"好"}

event: stream-data
data: {"type":"CHUNK","content":"！"}

event: stream-complete
data: {"type":"COMPLETE","totalTimeMs":1250,"stepCount":5}
```


---

### 方式二：前端 JavaScript 调用示例

```javascript
// 创建 EventSource 连接 SSE
const eventSource = new EventSource(
  'http://localhost:8080/api/decision/chat-stream/2034932406663692289?query=你好'
);

// 监听不同类型的 SSE 事件
eventSource.addEventListener('stream-start', (event) => {
  const data = JSON.parse(event.data);
  console.log('流式响应开始:', data.data);
});

eventSource.addEventListener('stream-progress', (event) => {
  const data = JSON.parse(event.data);
  console.log(`进度：${data.step}/${data.totalSteps} - ${data.message}`);
});

eventSource.addEventListener('stream-data', (event) => {
  const data = JSON.parse(event.data);
  // 逐字追加到显示区域
  appendToDisplay(data.content);
});

eventSource.addEventListener('stream-complete', (event) => {
  const data = JSON.parse(event.data);
  console.log(`响应完成，总耗时：${data.totalTimeMs}ms`);
  eventSource.close();
});

eventSource.addEventListener('stream-error', (event) => {
  const data = JSON.parse(event.data);
  console.error('发生错误:', data.message);
  eventSource.close();
});

// 辅助函数：追加文本到显示区域
function appendToDisplay(text) {
  const displayElement = document.getElementById('response-display');
  displayElement.textContent += text;
}
```


---

### 方式三：React 组件示例

```jsx
import { useState, useEffect, useRef } from 'react';

function StreamingChat({ agentId, query }) {
  const [response, setResponse] = useState('');
  const [status, setStatus] = useState('idle'); // idle, loading, complete, error
  const [progress, setProgress] = useState({ step: 0, totalSteps: 5 });
  const eventSourceRef = useRef(null);

  useEffect(() => {
    if (!query) return;

    setStatus('loading');
    setResponse('');
    
    // 创建 SSE 连接
    const url = `http://localhost:8080/api/decision/chat-stream/${agentId}?query=${encodeURIComponent(query)}`;
    eventSourceRef.current = new EventSource(url);

    const es = eventSourceRef.current;

    es.addEventListener('stream-progress', (event) => {
      const data = JSON.parse(event.data);
      setProgress({ step: data.step, totalSteps: data.totalSteps });
    });

    es.addEventListener('stream-data', (event) => {
      const data = JSON.parse(event.data);
      setResponse(prev => prev + data.content);
    });

    es.addEventListener('stream-complete', (event) => {
      const data = JSON.parse(event.data);
      setStatus('complete');
      console.log(`完成，耗时：${data.totalTimeMs}ms`);
      es.close();
    });

    es.addEventListener('stream-error', (event) => {
      const data = JSON.parse(event.data);
      setStatus('error');
      console.error('错误:', data.message);
      es.close();
    });

    // 清理函数
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [agentId, query]);

  return (
    <div className="streaming-chat">
      <div className="status">
        {status === 'loading' && `处理中：${progress.step}/${progress.totalSteps}`}
        {status === 'complete' && '回答完成'}
        {status === 'error' && '发生错误'}
      </div>
      <div className="response">{response}</div>
    </div>
  );
}

export default StreamingChat;
```


---

### 方式四：Vue 3 组件示例

```vue
<template>
  <div class="streaming-chat">
    <div class="status">
      <span v-if="status === 'loading'">
        处理中：{{ progress.step }}/{{ progress.totalSteps }}
      </span>
      <span v-else-if="status === 'complete'">回答完成</span>
      <span v-else-if="status === 'error'">发生错误</span>
    </div>
    <div class="response">{{ response }}</div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue';

const props = defineProps({
  agentId: Number,
  query: String
});

const response = ref('');
const status = ref('idle');
const progress = ref({ step: 0, totalSteps: 5 });
let eventSource = null;

watch(() => props.query, (newQuery) => {
  if (!newQuery) return;

  status.value = 'loading';
  response.value = '';
  
  const url = `http://localhost:8080/api/decision/chat-stream/${props.agentId}?query=${encodeURIComponent(newQuery)}`;
  eventSource = new EventSource(url);

  eventSource.addEventListener('stream-progress', (event) => {
    const data = JSON.parse(event.data);
    progress.value = { step: data.step, totalSteps: data.totalSteps };
  });

  eventSource.addEventListener('stream-data', (event) => {
    const data = JSON.parse(event.data);
    response.value += data.content;
  });

  eventSource.addEventListener('stream-complete', (event) => {
    const data = JSON.parse(event.data);
    status.value = 'complete';
    eventSource.close();
  });

  eventSource.addEventListener('stream-error', (event) => {
    const data = JSON.parse(event.data);
    status.value = 'error';
    eventSource.close();
  });
}, { immediate: true });

onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
  }
});
</script>
```


---

## 技术细节

### Flux 订阅机制

```java
// 在 DecisionExecutorImpl 中的实现
Flux<String> flux = aiModelSupport.streamChatModel(
    aiModelSupport.createChatModel(aiModel, provider), 
    query, 
    combinedResponse, 
    agent
);

flux.subscribe(
    // onNext: 接收每个文本块
    content -> {
        try {
            sseEventPublisher.streamResponse(emitter, content);
        } catch (Exception e) {
            log.error("发送流式响应失败", e);
        }
    },
    // onError: 处理错误
    error -> {
        log.error("Flux 流式处理异常", error);
        sseEventPublisher.sendError(emitter, "流式处理失败：" + error.getMessage());
    },
    // onComplete: 完成回调
    () -> {
        traceRecorder.recordCallChainSummary(traceContext, agentId, System.currentTimeMillis() - startTime);
        sseEventPublisher.sendCompleted(emitter, System.currentTimeMillis() - startTime, decisionPath.size());
        emitter.complete();
        log.info("流式决策执行完成，totalTime: {}ms", System.currentTimeMillis() - startTime);
    }
);
```


### SSE 事件发布器

**位置**: `com.esdllm.agentmesh.service.agent.support.SseEventPublisher`

**主要方法**:

```java
// 发送进度更新
void sendProgress(SseEmitter emitter, int currentStep, int totalSteps, String message);

// 发送意图识别结果
void sendIntentRecognized(SseEmitter emitter, IntentRecognitionResult intent, long durationMs);

// 发送工具匹配结果
void sendToolMatched(SseEmitter emitter, List<Tools> tools, long durationMs);

// 发送工具调用开始
void sendToolCallStarted(SseEmitter emitter, Tools tool);

// 发送工具调用完成
void sendToolCallCompleted(SseEmitter emitter, Tools tool, String response, long durationMs);

// 流式响应数据块
void streamResponse(SseEmitter emitter, String content);

// 发送完成事件
void sendCompleted(SseEmitter emitter, long totalTimeMs, int stepCount);

// 发送错误事件
void sendError(SseEmitter emitter, String errorMessage);
```


---

## 错误处理

### 常见错误及处理

1. **连接超时**

   ```event
   event: stream-error
   data: {"type":"ERROR","message":"请求处理超时","timestamp":1710924373310}
   ```


2. **业务异常**

   ```event
   event: stream-error
   data: {"type":"ERROR","message":"智能体不存在","timestamp":1710924373310}
   ```


3. **系统异常**

   ```event
   event: stream-error
   data: {"type":"ERROR","message":"处理失败：系统内部错误","timestamp":1710924373310}
   ```


### 客户端重连机制

```javascript
// 自动重连示例
function createResilientEventSource(url, maxRetries = 3) {
  let retries = 0;
  let eventSource = null;

  function connect() {
    eventSource = new EventSource(url);
    
    eventSource.addEventListener('stream-complete', () => {
      retries = 0; // 重置重试计数
      eventSource.close();
    });

    eventSource.addEventListener('stream-error', () => {
      eventSource.close();
      
      if (retries < maxRetries) {
        retries++;
        console.log(`重连 ${retries}/${maxRetries}`);
        setTimeout(connect, 2000 * retries); // 指数退避
      }
    });

    eventSource.onerror = () => {
      eventSource.close();
      
      if (retries < maxRetries) {
        retries++;
        console.log(`连接失败，重连 ${retries}/${maxRetries}`);
        setTimeout(connect, 2000 * retries);
      }
    };
  }

  connect();
  
  return {
    close: () => eventSource?.close(),
    addEventListener: (type, handler) => eventSource?.addEventListener(type, handler)
  };
}
```


---

## 性能优化建议

### 1. 客户端缓冲

```javascript
// 使用防抖减少渲染频率
function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
}
```


### 2. 服务端超时配置

```yaml
# application.yml
spring:
  ai:
    chat:
      client:
        connection-timeout: 30s
        read-timeout: 120s
```


### 3. SSE Emitter 配置

```java
// 设置合理的超时时间
SseEmitter emitter = new SseEmitter(120000L); // 120 秒超时
```


---

## 最佳实践

### ✅ 推荐做法

1. **始终监听错误事件**

   ```javascript
   eventSource.addEventListener('stream-error', handleError);
   ```


2. **手动关闭连接**

   ```javascript
   eventSource.addEventListener('stream-complete', () => {
     eventSource.close();
   });
   ```


3. **显示处理进度**

   ```javascript
   eventSource.addEventListener('stream-progress', updateProgressBar);
   ```


4. **处理网络中断**

   ```javascript
   eventSource.onerror = handleConnectionError;
   ```


### ❌ 避免做法

1. **不要忽略错误处理**
2. **不要忘记关闭连接**
3. **不要频繁创建 EventSource**（应复用）
4. **不要在连接未关闭时重复发起请求**

---

## 调试技巧

### 浏览器开发者工具

```javascript
// 在控制台监听所有 SSE 事件
const es = new EventSource(url);
es.onmessage = (e) => console.log('Message:', e.data);
es.addEventListener('stream-data', (e) => console.log('Chunk:', e.data));
es.addEventListener('error', (e) => console.error('Error:', e));
```


### 日志记录

```javascript
// 记录所有接收到的事件
const events = [];
eventSource.addEventListener('stream-data', (e) => {
  events.push(JSON.parse(e.data));
  console.log(`[${events.length}] 收到文本块:`, e.data);
});
```


---

## 完整示例

### Node.js 后端调用示例

```javascript
const axios = require('axios');

async function streamingChat(agentId, query) {
  const response = await axios.get(
    `http://localhost:8080/api/decision/chat-stream/${agentId}`,
    {
      params: { query },
      responseType: 'stream',
      headers: { 'Accept': 'text/event-stream' }
    }
  );

  response.data.on('data', (chunk) => {
    const lines = chunk.toString().split('\n');
    
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.substring(6));
        
        if (line.startsWith('event: stream-data')) {
          process.stdout.write(data.content);
        } else if (line.startsWith('event: stream-complete')) {
          console.log(`\n完成，耗时：${data.totalTimeMs}ms`);
        }
      }
    }
  });
}

// 使用示例
streamingChat(2034932406663692289, '你好');
```


---

## 总结

本流式 API 基于 Spring AI Flux 实现，提供以下特性：

✅ **实时响应** - 逐字输出，降低等待焦虑  
✅ **进度可见** - 实时展示处理步骤  
✅ **错误友好** - 详细的错误信息和恢复机制  
✅ **跨平台** - 支持各种前端框架  
✅ **高性能** - 异步非阻塞，支持背压

通过 SSE (Server-Sent Events) 协议，客户端可以实时接收服务端的流式数据，获得更好的用户体验。