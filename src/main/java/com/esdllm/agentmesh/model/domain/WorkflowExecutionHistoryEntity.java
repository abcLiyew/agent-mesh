package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 工作流执行历史实体
 */
@TableName(value = "workflow_execution_history")
@Data
public class WorkflowExecutionHistoryEntity {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 工作流ID
     */
    private Long workflowId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 输入参数(JSON)
     */
    private Object inputParamsJson;
    
    /**
     * 输出结果(JSON)
     */
    private Object outputResultJson;
    
    /**
     * 执行路径(JSON)
     */
    private Object executionPathJson;
    
    /**
     * 节点执行结果(JSON)
     */
    private Object nodeResultsJson;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 总耗时(毫秒)
     */
    private Long totalDurationMs;
    
    /**
     * 开始时间
     */
    private Date startedAt;
    
    /**
     * 完成时间
     */
    private Date completedAt;
}
