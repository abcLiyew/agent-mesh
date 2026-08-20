package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 知识库主表
 */
@TableName(value = "knowledge_base")
@Data
public class KnowledgeBase {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 归属用户 ID：知识库的创建者
     */
    private Long userId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 向量存储类型：[DASHSCOPE, OLLAMA, OPENAI]
     */
    private String vectorStoreType;

    /**
     * 对应的向量存储表名
     */
    private String vectorStoreTable;

    /**
     * 嵌入模型 ID：用于生成向量
     */
    private Long embeddingModelId;

    /**
     * 文本分块大小
     */
    private Integer chunkSize;

    /**
     * 分块重叠大小
     */
    private Integer chunkOverlap;

    /**
     * 状态:1=启用,0=停用
     */
    private Integer status;

    /**
     * 逻辑删除标记：0=正常，1=已删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;
}
