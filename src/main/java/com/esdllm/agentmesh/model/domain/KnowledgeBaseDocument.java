package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 知识库文档表
 */
@TableName(value = "knowledge_base_document", autoResultMap = true)
@Data
public class KnowledgeBaseDocument {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 所属知识库 ID
     */
    private Long kbId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档类型：[TEXT, PDF, WORD, EXCEL, MARKDOWN, URL]
     */
    private String docType;

    /**
     * 源文件 URL 或路径
     */
    private String sourceUrl;

    /**
     * 内容哈希：用于去重和版本控制
     */
    private String contentHash;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 向量 ID 列表：存储在向量数据库中的 ID 数组
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<String> vectorIds;

    /**
     * 元数据：作者、创建时间等额外信息
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object metadataJson;

    /**
     * 关联工具 ID 列表：JSON 数组格式，用于 RAG 驱动的工具推荐
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object relatedToolIds;

    /**
     * 状态：1=处理完成，0=处理中，-1=处理失败
     */
    private Integer status;

    /**
     * 逻辑删除标记：0=正常，1=已删除
     */
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
