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
 * 技能包定义实体
 * 对应"龙虾"的技能市场机制，支持能力包的动态加载和集成
 */
@TableName("agent_skill_package")
@Data
@Schema(description = "技能包定义实体")
public class AgentSkillPackage {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    /**
     * 技能名称
     */
    @Schema(description = "技能名称")
    private String skillName;
    
    /**
     * 唯一技能代码
     */
    @Schema(description = "唯一技能代码")
    private String skillCode;
    
    /**
     * 技能描述
     */
    @Schema(description = "技能描述")
    private String description;
    
    /**
     * 技能分类：data_analysis, content_generation, automation, integration
     */
    @Schema(description = "技能分类")
    private String category;
    
    /**
     * 版本号
     */
    @Schema(description = "版本号")
    private String version;
    
    /**
     * 作者用户ID
     */
    @Schema(description = "作者用户ID")
    private Long authorId;
    
    /**
     * 技能配置JSON(包含工具列表、参数模板等)
     */
    @Schema(description = "技能配置JSON")
    private String skillConfigJson;
    
    /**
     * 输入参数Schema JSON
     */
    @Schema(description = "输入参数Schema")
    private String inputSchemaJson;
    
    /**
     * 输出结果Schema JSON
     */
    @Schema(description = "输出结果Schema")
    private String outputSchemaJson;
    
    /**
     * 使用示例
     */
    @Schema(description = "使用示例")
    private String exampleUsage;
    
    /**
     * 图标URL
     */
    @Schema(description = "图标URL")
    private String iconUrl;
    
    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Integer downloadCount;
    
    /**
     * 平均评分
     */
    @Schema(description = "平均评分")
    private BigDecimal ratingAvg;
    
    /**
     * 评分次数
     */
    @Schema(description = "评分次数")
    private Integer ratingCount;
    
    /**
     * 状态: 1=发布, 0=草稿, -1=下架
     */
    @Schema(description = "状态")
    private Integer status;
    
    /**
     * 是否公开共享
     */
    @Schema(description = "是否公开")
    private Boolean isPublic;
    
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
     * 逻辑删除标记
     */
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
