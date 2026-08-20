package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import com.esdllm.agentmesh.model.dto.DecisionStep;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 对话日志表
 */
@TableName(value = "conversation_log", autoResultMap = true)
@Data
public class ConversationLog {
    
    @TableId
    private Long id;
    
    private Long userId;
    
    private Long agentId;
    
    private String sessionId;
    
    private String userQuery;
    
    private String finalResponse;
    
    private String intentType;
    
    private BigDecimal intentConfidence;
    
    /**
     * 决策路径（JSON 格式）
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<DecisionStep> decisionPath;
    
    /**
     * 调用的工具 ID 列表
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Long> invokedToolIds;
    
    /**
     * 检索的知识库 ID 列表
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Long> searchedKbIds;
    
    /**
     * 使用的决策模型 ID
     */
    private Long decisionModelId;
    
    /**
     * 使用的回复模型 ID
     */
    private Long responseModelId;
    
    /**
     * 总输入 Token 数
     */
    private Integer totalInputTokens;
    
    /**
     * 总输出 Token 数
     */
    private Integer totalOutputTokens;
    
    /**
     * 总成本（元）
     */
    private BigDecimal totalCost;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;
    
    /**
     * 对话状态：SUCCESS, FAILED, PARTIAL_SUCCESS
     */
    private Integer status;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 用户反馈评分（1-5 星）
     */
    private Integer userRating;
    
    /**
     * 用户反馈备注
     */
    private String userFeedback;
    
    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
