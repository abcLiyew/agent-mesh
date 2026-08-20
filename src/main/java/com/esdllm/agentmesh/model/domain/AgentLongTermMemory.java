package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 智能体长期记忆实体
 * 存储跨会话、跨周期的关键信息：用户偏好、项目背景、决策逻辑等
 */
@TableName("agent_long_term_memory")
@Data
@Schema(description = "智能体长期记忆实体")
public class AgentLongTermMemory {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;
    
    /**
     * 记忆类型: preference, interaction_pattern, domain_knowledge, feedback_insight
     */
    @Schema(description = "记忆类型")
    private String memoryType;
    
    /**
     * 记忆键,用于快速检索
     */
    @Schema(description = "记忆键")
    private String memoryKey;
    
    /**
     * 记忆值
     */
    @Schema(description = "记忆值")
    private String memoryValue;
    
    /**
     * 记忆向量,用于相似度检索
     */
    @Schema(description = "记忆向量")
    private float[] memoryVector;
    
    /**
     * 置信度 0-1
     */
    @Schema(description = "置信度")
    private BigDecimal confidenceScore;
    
    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    private Integer usageCount;
    
    /**
     * 最后使用时间
     */
    @Schema(description = "最后使用时间")
    private LocalDateTime lastUsedAt;
    
    /**
     * 来源类型: explicit_feedback, implicit_observation, llm_extraction
     */
    @Schema(description = "来源类型")
    private String sourceType;
    
    /**
     * 来源引用ID
     */
    @Schema(description = "来源引用ID")
    private Long sourceReferenceId;
    
    /**
     * 标签数组(JSON)
     */
    @Schema(description = "标签数组")
    private String tags;
    
    /**
     * 是否激活
     */
    @Schema(description = "是否激活")
    private Boolean isActive;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createdAt;
    
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updatedAt;
    
    /**
     * 过期时间,NULL表示永久有效
     */
    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;
}
