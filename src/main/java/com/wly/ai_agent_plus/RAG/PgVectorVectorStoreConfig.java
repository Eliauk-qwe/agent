package com.wly.ai_agent_plus.RAG;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 旧的 PGVector 配置 - 已被 LoveAppVectorStoreConfig 替代
 * 
 * 问题：
 * 1. 维度设置错误（1536 应该是 1024，通义千问输出 1024 维）
 * 2. 没有处理文档加载逻辑
 * 3. 没有检查数据是否已存在，每次启动都会重复加载
 * 4. 没有创建 pgvector 扩展
 * 
 * 新配置在 LoveAppVectorStoreConfig.java
 */
// @Configuration  // 已注释，使用 LoveAppVectorStoreConfig 替代
public class PgVectorVectorStoreConfig {

    // @Bean  // 已注释
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)                    // 错误：应该是 1024（通义千问维度）
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
        return vectorStore;
    }
}

