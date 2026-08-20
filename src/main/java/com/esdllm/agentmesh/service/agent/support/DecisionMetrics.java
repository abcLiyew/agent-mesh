package com.esdllm.agentmesh.service.agent.support;

import com.esdllm.agentmesh.model.dto.DecisionStep;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 执行器监控指标
 */
@Component
@Slf4j
public class DecisionMetrics {

    private final MeterRegistry meterRegistry;

    private final ConcurrentLinkedQueue<Long> executionQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    public DecisionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    /**
     * 初始化监控指标
     */
    private void initMetrics() {
        // 1. 执行队列大小（实时）
        Gauge.builder("agent.executor.queue.size", this, m -> executionQueue.size())
            .description("当前等待执行的请求数量")
            .register(meterRegistry);

        // 2. 总执行次数（计数器）
        Gauge.builder("agent.executor.total.executions", this,
                m -> (double) totalExecutions.get())
            .description("累计执行次数")
            .register(meterRegistry);

        // 3. 失败执行次数（计数器）
        Gauge.builder("agent.executor.failed.executions", this,
                m -> (double) failedExecutions.get())
            .description("累计失败次数")
            .register(meterRegistry);
    }

    /**
     * 记录执行耗时
     */
    public void recordExecutionTime(long timeMs, boolean success) {
        executionQueue.offer(System.currentTimeMillis());
        executionQueue.poll(); // 保持队列大小稳定

        totalExecutions.incrementAndGet();
        if (!success) {
            failedExecutions.incrementAndGet();
        }

        Timer timer = Timer.builder("agent.executor.execution.time")
            .description("决策执行耗时分布")
            .tag("success", success ? "true" : "false")
            .tag("type", "decision")
            .register(meterRegistry);

        timer.record(timeMs, TimeUnit.MILLISECONDS);

        log.debug("记录执行指标：耗时 {}ms, 成功：{}", timeMs, success);
    }

    /**
     * 记录工具调用指标
     */
    public void recordToolCall(String toolType, long timeMs, boolean success) {
        Timer timer = Timer.builder("agent.tool.call.time")
            .description("工具调用耗时")
            .tag("tool_type", toolType)
            .tag("success", success ? "true" : "false")
            .register(meterRegistry);

        timer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录意图识别准确率
     */
    public void recordIntentRecognition(String intentType, double confidence, boolean correct) {
        meterRegistry.counter("agent.intent.recognition.total").increment();
        if (correct) {
            meterRegistry.counter("agent.intent.recognition.correct").increment();
        }

        Gauge.builder("agent.intent.confidence.last", this,
                m -> confidence)
            .tag("intent_type", intentType)
            .description("最近一次意图识别的置信度")
            .register(meterRegistry);
    }

    /**
     * 记录决策路径指标
     */
    public void recordDecisionPath(List<DecisionStep> decisionPath) {
        for (DecisionStep step : decisionPath) {
            if ("TOOL_CALL".equals(step.getStepType())) {
                recordToolCall("general", step.getDurationMs(), 
                    step.getOutputData() != null && !step.getOutputData().toString().contains("异常"));
            } else if ("INTENT_RECOGNITION".equals(step.getStepType())) {
                Object confidence = ((Map<?, ?>) step.getOutputData()).get("confidence");
                if (confidence != null) {
                    recordIntentRecognition(
                        ((Map<?, ?>) step.getOutputData()).get("intent_type").toString(),
                        Double.parseDouble(confidence.toString()),
                        true
                    );
                }
            }
        }
    }
}
