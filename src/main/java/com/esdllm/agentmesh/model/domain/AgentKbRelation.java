package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 智能体 - 知识库关联表
 */
@TableName(value = "agent_kb_relation")
@Data
public class AgentKbRelation {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 智能体 ID
     */
    private Long agentId;

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 检索返回的最大结果数
     */
    private Integer searchTopK;

    /**
     * 相似度阈值
     */
    private BigDecimal similarityThreshold;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 逻辑删除标记：0=正常，1=已删除
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
