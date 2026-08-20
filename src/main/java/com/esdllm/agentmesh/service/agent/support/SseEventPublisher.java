package com.esdllm.agentmesh.service.agent.support;

import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;
import com.esdllm.agentmesh.model.dto.sse.ProgressEvent;
import com.esdllm.agentmesh.model.dto.sse.SseEvent;
import com.esdllm.agentmesh.model.dto.sse.ToolCallEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSE 事件推送器：统一处理所有 SSE 事件发送逻辑
 */
@Service
@Slf4j
public class SseEventPublisher {

    @Resource
    private SseEventFormatter eventFormatter;

    /**
     * 发送进度事件
     */
    public void sendProgress(SseEmitter emitter, int currentStep, int totalSteps, String description) {
        if (isNotEmitterAlive(emitter)) {
            log.warn("SSE 连接已断开，跳过发送进度事件");
            return;
        }
        
        try {
            double progress = (double) currentStep / totalSteps * 100;
            ProgressEvent progressEvent = ProgressEvent.builder()
                    .currentStep(currentStep)
                    .totalSteps(totalSteps)
                    .progress(progress)
                    .stepDescription(description)
                    .estimatedRemainingMs((totalSteps - currentStep) * 3000L)
                    .build();

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.PROGRESS)
                    .data(eventFormatter.format(SseEvent.EventTypes.PROGRESS, progressEvent)));

            log.debug("发送进度事件：step {}/{} - {}", currentStep, totalSteps, description);
        } catch (IOException e) {
            log.error("发送进度事件失败", e);
            completeWithError(emitter, "消息推送失败");
        }
    }

    /**
     * 发送意图识别事件（增强版）
     */
    public void sendIntentRecognized(SseEmitter emitter, IntentRecognitionResult intent, long durationMs) {
        try {
            Map<String, Object> data = Map.of(
                    "stepId", generateStepId("INTENT"),
                    "stepType", "INTENT_RECOGNITION",
                    "intentType", intent.getIntentType(),
                    "confidence", intent.getConfidence() != null ? intent.getConfidence().doubleValue() : 0.0,
                    "needToolCall", intent.getNeedToolCall(),
                    "durationMs", durationMs,
                    "status", "COMPLETED",
                    "description", "意图识别完成"
            );

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.INTENT_RECOGNIZED)
                    .data(eventFormatter.format(SseEvent.EventTypes.INTENT_RECOGNIZED, data)));
        } catch (IOException e) {
            log.error("发送意图识别事件失败", e);
        }
    }

    /**
     * 发送工具匹配事件（增强版）
     */
    public void sendToolMatched(SseEmitter emitter, List<Tools> tools, long durationMs) {
        try {
            Map<String, Object> data = Map.of(
                    "stepId", generateStepId("MATCH"),
                    "stepType", "TOOL_MATCHING",
                    "matchedCount", tools.size(),
                    "toolIds", tools.stream().map(Tools::getId).toList(),
                    "toolNames", tools.stream().map(Tools::getDisplayName).toList(),
                    "durationMs", durationMs,
                    "status", "COMPLETED",
                    "description", "工具匹配完成"
            );

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.TOOL_MATCHING)
                    .data(eventFormatter.format(SseEvent.EventTypes.TOOL_MATCHING, data)));
        } catch (IOException e) {
            log.error("发送工具匹配事件失败", e);
        }
    }

    /**
     * 发送工具调用开始事件（增强版）
     */
    public void sendToolCallStarted(SseEmitter emitter, Tools tool) {
        try {
            ToolCallEvent event = ToolCallEvent.builder()
                    .stepId(generateStepId("TOOL_" + tool.getId()))
                    .toolId(tool.getId())
                    .toolName(tool.getDisplayName())
                    .toolType(tool.getSourceType())
                    .status("RUNNING")
                    .build();

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.TOOL_CALL_START)
                    .data(eventFormatter.format(SseEvent.EventTypes.TOOL_CALL_START, event)));
        } catch (IOException e) {
            log.error("发送工具调用开始事件失败", e);
        }
    }

    /**
     * 发送工具调用完成事件（增强版）
     */
    public void sendToolCallCompleted(SseEmitter emitter, Tools tool, String result, long durationMs) {
        try {
            ToolCallEvent event = ToolCallEvent.builder()
                    .stepId(generateStepId("TOOL_" + tool.getId()))
                    .toolId(tool.getId())
                    .toolName(tool.getDisplayName())
                    .toolType(tool.getSourceType())
                    .status("COMPLETED")
                    .result(result)
                    .durationMs(durationMs)
                    .build();

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.TOOL_CALL_COMPLETE)
                    .data(eventFormatter.format(SseEvent.EventTypes.TOOL_CALL_COMPLETE, event)));
        } catch (IOException e) {
            log.error("发送工具调用完成事件失败", e);
        }
    }

    /**
     * 发送工具调用错误事件（增强版）
     */
    public void sendToolCallFailed(SseEmitter emitter, Tools tool, String errorMessage) {
        try {
            ToolCallEvent event = ToolCallEvent.builder()
                    .stepId(generateStepId("TOOL_" + tool.getId()))
                    .toolId(tool.getId())
                    .toolName(tool.getDisplayName())
                    .toolType(tool.getSourceType())
                    .status("FAILED")
                    .errorMessage(errorMessage)
                    .build();

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.ERROR)
                    .data(eventFormatter.format(SseEvent.EventTypes.ERROR, event)));
        } catch (IOException e) {
            log.error("发送工具调用错误事件失败", e);
        }
    }

    /**
     * 流式推送回答内容（增强版）
     */
    public void streamResponse(SseEmitter emitter, String response) {
        if (response == null || response.isEmpty()) {
            return;
        }
        
        try {
            int chunkSize = 50;
            for (int i = 0; i < response.length(); i += chunkSize) {
                // 检查 emitter 是否还存活
                if (isNotEmitterAlive(emitter)) {
                    log.info("客户端断开连接，终止流式推送");
                    break;
                }
                
                String chunk = response.substring(i, Math.min(i + chunkSize, response.length()));

                try {
                    emitter.send(SseEmitter.event()
                            .name(SseEvent.EventTypes.RESPONSE_TOKEN)
                            .data(eventFormatter.format(SseEvent.EventTypes.RESPONSE_TOKEN, Map.of(
                                    "content", chunk,
                                    "position", i,
                                    "totalLength", response.length()
                            ))));
                } catch (IllegalStateException e) {
                    // Emitter 已经关闭，停止推送
                    log.debug("SSE 连接已关闭，停止推送：{}", e.getMessage());
                    break;
                } catch (IOException e) {
                    // 发送失败，可能是客户端断开
                    log.warn("发送 SSE 数据失败，可能客户端已断开：{}", e.getMessage());
                    break;
                }

                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("流式推送被中断", e);
            completeWithError(emitter, "流式推送被中断");
        } catch (Exception e) {
            log.error("流式推送回答失败", e);
            // 不要再次调用 completeWithError，因为 emitter 可能已经关闭
        }
    }

    /**
     * 发送完成事件（增强版）
     */
    public void sendCompleted(SseEmitter emitter, long totalTimeMs, int totalSteps) {
        try {
            Map<String, Object> data = Map.of(
                    "status", "SUCCESS",
                    "totalTimeMs", totalTimeMs,
                    "totalSteps", totalSteps,
                    "message", "处理完成"
            );

            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.COMPLETE)
                    .data(eventFormatter.format(SseEvent.EventTypes.COMPLETE, data)));
        } catch (IOException e) {
            log.error("发送完成事件失败", e);
        }
    }

    /**
     * 发送错误事件
     */
    public void sendError(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name(SseEvent.EventTypes.ERROR)
                    .data(eventFormatter.format(SseEvent.EventTypes.ERROR, Map.of(
                            "error", errorMessage,
                            "timestamp", System.currentTimeMillis()
                    ))));
        } catch (IOException e) {
            log.error("发送错误事件失败", e);
        }
    }

    /**
     * 检查 SSE 连接是否存活
     */
    private boolean isNotEmitterAlive(SseEmitter emitter) {
        return emitter == null;
    }

    /**
     * 完成并关闭连接（带错误信息）
     */
    private void completeWithError(SseEmitter emitter, String errorMessage) {
        try {
            sendError(emitter, errorMessage);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 生成步骤 ID
     */
    private String generateStepId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}
