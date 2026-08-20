package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolVersionDTO {
    
    private Long id;
    
    private Long toolId;
    
    private String versionNumber;
    
    private String versionName;
    
    private String description;
    
    private Boolean isActive;
    
    private Boolean isCurrent;
    
    private Long parentVersionId;
    
    private String changeLog;
    
    private Long createdBy;
    
    private Long createdAt;
}
