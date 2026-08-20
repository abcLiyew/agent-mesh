package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 智能体 - 工具关联表：定义某个智能体可以使用哪些工具 (多对多关系)
 * @TableName agent_tool_relation
 */
@TableName(value ="agent_tool_relation")
@Data
public class AgentToolRelation {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 所属智能体 ID：关联 agent 表，级联删除
     */
    private Long agentId;

    /**
     * 工具来源类型：冗余字段，记录工具来源 (SYSTEM/USER_HTTP/USER_MCP)，便于快速过滤
     */
    private String toolType;

    /**
     * 工具引用 ID：关联 tools 表的主键
     */
    private Long toolRefId;

    /**
     * 工具特定配置：JSON 格式，针对该智能体对该工具的参数覆写 (如 timeout, 特定 key)
     */
    private Object configParams;

    /**
     * 排序顺序：工具在列表中的显示顺序或调用优先级
     */
    private Integer sortOrder;

    /**
     * 逻辑删除标记：0=正常, 1=已删除 (用于暂时移除工具)
     */
    private Integer isDelete;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        AgentToolRelation other = (AgentToolRelation) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAgentId() == null ? other.getAgentId() == null : this.getAgentId().equals(other.getAgentId()))
            && (this.getToolType() == null ? other.getToolType() == null : this.getToolType().equals(other.getToolType()))
            && (this.getToolRefId() == null ? other.getToolRefId() == null : this.getToolRefId().equals(other.getToolRefId()))
            && (this.getConfigParams() == null ? other.getConfigParams() == null : this.getConfigParams().equals(other.getConfigParams()))
            && (this.getSortOrder() == null ? other.getSortOrder() == null : this.getSortOrder().equals(other.getSortOrder()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAgentId() == null) ? 0 : getAgentId().hashCode());
        result = prime * result + ((getToolType() == null) ? 0 : getToolType().hashCode());
        result = prime * result + ((getToolRefId() == null) ? 0 : getToolRefId().hashCode());
        result = prime * result + ((getConfigParams() == null) ? 0 : getConfigParams().hashCode());
        result = prime * result + ((getSortOrder() == null) ? 0 : getSortOrder().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", agentId=").append(agentId);
        sb.append(", toolType=").append(toolType);
        sb.append(", toolRefId=").append(toolRefId);
        sb.append(", configParams=").append(configParams);
        sb.append(", sortOrder=").append(sortOrder);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}