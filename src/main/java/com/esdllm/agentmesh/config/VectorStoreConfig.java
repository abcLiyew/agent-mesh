package com.esdllm.agentmesh.config;

import lombok.Data;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vector-store.pgvector")
@Data
public class VectorStoreConfig {

    public enum DimensionsType{
        DIMENSIONS_OPENAI(1536),
        DIMENSIONS_BAGLARGEZH(1024),
        DIMENSIONS_BAIDU(768);
        private final Integer value;
        DimensionsType(Integer value){
            this.value = value;
        }
    }

    /**
     * 表名
     */
    private String vectorTableName;
    /**
     * 是否初始化向量存储表
     */
    private Boolean initializeSchema;
    /**
     * 是否删除已有的向量存储表
     */
    private Boolean removeExistingVectorStoreTable = false;
    /**
     * 索引类型
     */
    private PgVectorStore.PgIndexType indexType;
    /**
     * 向量维度
     */
    private DimensionsType dimensionsType;
    /**
     * 向量距离类型
     */
    private PgVectorStore.PgDistanceType distanceType;
    /**
     * 最大文档批量大小
     */
    private Integer maxDocumentBatchSize=1000;
    /**
     * 模式名
     */
    private String schemaName="public";



    public PgVectorStore dashscopeVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .vectorTableName("dashscope_"+vectorTableName)          // 默认值，可省略
                .schemaName(schemaName)                     // 默认值，可省略
                .dimensions(dimensionsType.value)
                .distanceType(distanceType)
                .indexType(indexType)
                .initializeSchema(initializeSchema)                   // 第一次启动时自动建表（生产环境建议 false）
                .removeExistingVectorStoreTable(removeExistingVectorStoreTable)    // 切勿设为 true！会清空所有知识库数据
                .maxDocumentBatchSize(maxDocumentBatchSize)
                .build();
    }

    public PgVectorStore defaultOllamaVectorStore(JdbcTemplate jdbcTemplate,EmbeddingModel ollamaEmbeddingModel){
        return PgVectorStore.builder(jdbcTemplate,ollamaEmbeddingModel)
                .vectorTableName("ollama_"+vectorTableName)
                .schemaName(schemaName)
                .dimensions(dimensionsType.value)
                .distanceType(distanceType)
                .indexType(indexType)
                .initializeSchema(initializeSchema)
                .removeExistingVectorStoreTable(removeExistingVectorStoreTable)
                .maxDocumentBatchSize(maxDocumentBatchSize)
                .build();
    }

    public PgVectorStore openAiVectorStore(JdbcTemplate jdbcTemplate,EmbeddingModel openAiEmbeddingModel){
        return PgVectorStore.builder(jdbcTemplate,openAiEmbeddingModel)
                .vectorTableName("openai_"+vectorTableName)
                .schemaName(schemaName)
                .dimensions(dimensionsType.value)
                .distanceType(distanceType)
                .indexType(indexType)
                .initializeSchema(initializeSchema)
                .removeExistingVectorStoreTable(removeExistingVectorStoreTable)
                .maxDocumentBatchSize(maxDocumentBatchSize)
                .build();
    }
}
