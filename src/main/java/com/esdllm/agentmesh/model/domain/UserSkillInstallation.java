package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户技能安装记录
 */
@TableName("user_skill_installation")
@Data
@Schema(description = "用户技能安装记录")
public class UserSkillInstallation {
    
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
     * 技能包ID
     */
    @Schema(description = "技能包ID")
    private Long skillId;
    
    /**
     * 用户自定义的安装配置JSON
     */
    @Schema(description = "安装配置JSON")
    private String installationConfigJson;
    
    /**
     * 状态: 1=已安装, 0=已禁用
     */
    @Schema(description = "状态")
    private Integer status;
    
    /**
     * 安装时间
     */
    @Schema(description = "安装时间")
    private LocalDateTime installedAt;
    
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
