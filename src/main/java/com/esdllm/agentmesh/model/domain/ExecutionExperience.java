package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行经验实体 - 用于龙虾架构的学习和优化
 */
@TableName("execution_experience")
@Data
@Schema(description = "执行经验实体")
public class ExecutionExperience {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "关联的工作流ID")
    private Long workflowId;
    
    @Schema(description = "经验类型: workflow_execution, task_planning, tool_invocation")
    private String experienceType;
    
    @Schema(description = "执行是否成功")
    private Boolean success;
    
    @Schema(description = "用户评分 1-5")
    private Integer rating;
    
    @Schema(description = "用户反馈文本")
    private String userFeedback;
    
    @Schema(description = "执行耗时(毫秒)")
    private Long executionTimeMs;
    
    @Schema(description = "决策路径JSON快照")
    private String decisionPathJson;
    
    @Schema(description = "上下文摘要")
    private String contextSummary;
    
    @Schema(description = "学习到的模式(JSON格式)")
    private String learnedPatterns;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    
    @Schema(description = "是否激活")
    private Boolean isActive;
}
