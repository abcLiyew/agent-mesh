package com.esdllm.agentmesh.service.agent.support;

import com.esdllm.agentmesh.model.dto.sse.SseEvent;
import org.springframework.stereotype.Component;

/**
 * SSE 事件格式化器：统一构建 SSE 事件对象
 */
@Component
public class SseEventFormatter {

    /**
     * 格式化 SSE 事件
     */
    public <T> SseEvent format(String eventType, T data) {
        return SseEvent.builder()
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }
}
