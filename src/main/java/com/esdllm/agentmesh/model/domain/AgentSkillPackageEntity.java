package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 技能包定义实体
 */
@Data
@TableName("agent_skill_package")
@Schema(description = "技能包定义实体")
public class AgentSkillPackageEntity {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "技能名称")
    private String skillName;
    
    @Schema(description = "唯一技能代码")
    private String skillCode;
    
    @Schema(description = "技能描述")
    private String description;
    
    @Schema(description = "技能分类: data_analysis, content_generation, automation, integration")
    private String category;
    
    @Schema(description = "版本号")
    private String version;
    
    @Schema(description = "作者用户ID")
    private Long authorId;
    
    @Schema(description = "技能配置JSON")
    private String skillConfigJson;
    
    @Schema(description = "输入参数Schema JSON")
    private String inputSchemaJson;
    
    @Schema(description = "输出结果Schema JSON")
    private String outputSchemaJson;
    
    @Schema(description = "使用示例")
    private String exampleUsage;
    
    @Schema(description = "图标URL")
    private String iconUrl;
    
    @Schema(description = "下载次数")
    private Integer downloadCount;
    
    @Schema(description = "平均评分")
    private BigDecimal ratingAvg;
    
    @Schema(description = "评分次数")
    private Integer ratingCount;
    
    @Schema(description = "状态: 1=发布, 0=草稿, -1=下架")
    private Integer status;
    
    @Schema(description = "是否公开共享")
    private Boolean isPublic;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
