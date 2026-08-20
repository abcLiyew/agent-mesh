package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 最近活动日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    
    /**
     * 活动 ID
     */
    private Long id;
    
    /**
     * 活动类型：USER_REGISTER, AGENT_PUBLISH, TOOL_WARNING, MCP_ERROR, KB_UPDATE
     */
    private String activityType;
    
    /**
     * 活动标题
     */
    private String title;
    
    /**
     * 活动描述
     */
    private String description;
    
    /**
     * 活动状态：SUCCESS, WARNING, ERROR, INFO
     */
    private String status;
    
    /**
     * 发生时间
     */
    private LocalDateTime timestamp;
}
