package com.esdllm.agentmesh.model.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResult {
    
    /**
     * 执行 ID
     */
    private String executionId;
    
    /**
     * 工作流 ID
     */
    private Long workflowId;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 最终输出结果
     */
    private Object output;
    
    /**
     * 执行路径 (节点执行顺序)
     */
    private List<String> executionPath;
    
    /**
     * 每个节点的执行结果
     * key: nodeId, value: 节点执行结果
     */
    private Map<String, NodeExecutionResult> nodeResults;
    
    /**
     * 总耗时 (毫秒)
     */
    private Long totalDurationMs;
    
    /**
     * 错误信息 (如果失败)
     */
    private String errorMessage;
    
    /**
     * 全局变量最终状态
     */
    private Map<String, Object> finalVariables;
    
    /**
     * 节点执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeExecutionResult {
        /**
         * 节点 ID
         */
        private String nodeId;
        
        /**
         * 节点名称
         */
        private String nodeName;
        
        /**
         * 是否成功
         */
        private Boolean success;
        
        /**
         * 节点输出
         */
        private Object output;
        
        /**
         * 耗时 (毫秒)
         */
        private Long durationMs;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 重试次数
         */
        private Integer retryCount;
    }
}
