package com.esdllm.agentmesh.model.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流节点定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {
    
    /**
     * 节点 ID
     */
    private String nodeId;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点类型: TOOL_CALL, AGENT_CALL, CONDITION, PARALLEL, SEQUENCE
     */
    private NodeType nodeType;
    
    /**
     * 工具/智能体 ID (当类型为 TOOL_CALL 或 AGENT_CALL 时)
     */
    private Long resourceId;
    
    /**
     * 资源类型: TOOL 或 AGENT
     */
    private ResourceType resourceType;
    
    /**
     * 输入参数映射
     * key: 参数名, value: 表达式或常量
     */
    private Map<String, Object> inputParams;
    
    /**
     * 条件表达式 (当类型为 CONDITION 时)
     * 例如: "${result.status} == 'success'"
     */
    private String conditionExpression;
    
    /**
     * 条件为真时的下一个节点
     */
    private String trueBranch;
    
    /**
     * 条件为假时的下一个节点
     */
    private String falseBranch;
    
    /**
     * 子节点列表 (当类型为 PARALLEL 或 SEQUENCE 时)
     */
    private List<String> childNodes;
    
    /**
     * 是否并行执行 (仅对 PARALLEL 类型有效)
     */
    private Boolean parallel;
    
    /**
     * 超时时间 (毫秒)
     */
    private Long timeoutMs;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 错误处理策略: FAIL_FAST, CONTINUE, RETRY
     */
    private String errorStrategy;
    
    /**
     * 节点描述
     */
    private String description;
    
    public enum NodeType {
        TOOL_CALL,      // 调用工具
        AGENT_CALL,     // 调用智能体
        CONDITION,      // 条件判断
        PARALLEL,       // 并行执行
        SEQUENCE,       // 顺序执行
        START,          // 开始节点
        END             // 结束节点
    }
    
    public enum ResourceType {
        TOOL,
        AGENT
    }
}
