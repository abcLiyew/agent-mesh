package com.esdllm.agentmesh.model.dto.sse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 事件基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SSE 事件数据")
public class SseEvent {
    
    @Schema(description = "事件类型")
    private String eventType;
    
    @Schema(description = "事件时间戳")
    private Long timestamp;
    
    @Schema(description = "事件数据")
    private Object data;
    
    /**
     * 事件类型枚举
     */
    public static class EventTypes {
        public static final String START = "stream-start";
        public static final String INTENT_RECOGNIZED = "intent-recognized";
        public static final String TOOL_MATCHING = "tool-matching";
        public static final String TOOL_CALL_START = "tool-call-start";
        public static final String TOOL_CALL_COMPLETE = "tool-call-complete";
        public static final String KNOWLEDGE_SEARCH = "knowledge-search";
        public static final String MODEL_RESPONSE = "model-response";
        public static final String RESPONSE_TOKEN = "response-token";
        public static final String COMPLETE = "stream-complete";
        public static final String ERROR = "stream-error";
        public static final String PROGRESS = "stream-progress";
    }
}
