package com.wly.ai_agent_plus.RAG;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.util.List;

/**
 * 向量存储配置类 - 使用 PGVector（PostgreSQL 向量扩展）
 * 
 * 【已暂时注释】使用 SimpleVectorStore 替代
 * 如需启用 PGVector，取消注释 @Configuration 注解
 *
 * 为什么用 PGVector 而不是 SimpleVectorStore：
 *   SimpleVectorStore 把向量存在内存里，应用重启后向量丢失，
 *   每次启动都要重新调用 36 次 embedding API（耗时且耗费 token）。
 *   PGVector 把向量持久化到 PostgreSQL，重启后直接读数据库，不需要重新计算。
 *
 * 为什么用独立的 PostgreSQL，而不是复用 MySQL：
 *   MySQL 不支持向量存储，PGVector 是 PostgreSQL 的扩展插件。
 *   所以项目里有两个数据库：
 *     MySQL      → 存对话记忆（chat_message 表）
 *     PostgreSQL → 存向量数据（vector_store 表，由 PGVector 自动创建）
 *
 * 调用关系：
 *   应用启动
 *     → loveAppVectorStore Bean 初始化
 *       → 连接 PostgreSQL
 *       → 检查向量表是否已有数据
 *         → 有数据：直接用，不重新加载
 *         → 没数据：调用 LoveAppDocumentLoader 加载文档并写入
 */
// @Configuration  // 已暂时注释，使用 SimpleVectorStore
@Slf4j
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    // 向量维度，必须和 embedding 模型输出维度一致
    // 通义千问 text-embedding-v1/v2 输出 1024 维（不是 1536 维）
    @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}")
    private int dimensions;

    /**
     * 配置 PostgreSQL 数据源（独立于 MySQL 的第二个数据源）
     * @ConfigurationProperties 会自动读取 spring.pgvector.datasource.* 配置
     */
    @Bean
    @ConfigurationProperties("spring.pgvector.datasource")
    public DataSourceProperties pgVectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 创建 PostgreSQL DataSource
     */
    @Bean
    public DataSource pgVectorDataSource() {
        return pgVectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    /**
     * 创建 JdbcTemplate，PGVector 用它操作数据库
     */
    @Bean
    public JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(pgVectorDataSource());
    }

    /**
     * 创建 PGVector 向量存储 Bean
     *
     * 手动创建表结构，确保与 Spring AI 期望的结构一致
     * 
     * 【已暂时注释】使用 SimpleVectorStore 替代
     */
    // @Bean  // 已暂时注释
    VectorStore loveAppVectorStore(@NonNull EmbeddingModel embeddingModel) {
        log.info("初始化 PGVector 向量存储，向量维度: {}", dimensions);

        // 先检查并创建 pgvector 扩展
        try {
            pgVectorJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS vector");
            pgVectorJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS hstore");
            pgVectorJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
            log.info("PostgreSQL 扩展检查完成");
            
            // 删除旧表
            pgVectorJdbcTemplate().execute("DROP TABLE IF EXISTS public.vector_store");
            log.info("已清理旧的 vector_store 表");
            
            // 手动创建表（与 Spring AI PGVector 期望的结构一致）
            String createTableSql = String.format("""
                CREATE TABLE IF NOT EXISTS public.vector_store (
                    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
                    content text,
                    metadata json,
                    embedding vector(%d)
                )
                """, dimensions);
            pgVectorJdbcTemplate().execute(createTableSql);
            log.info("成功创建 vector_store 表");
            
            // 创建向量索引以提升查询性能
            pgVectorJdbcTemplate().execute(
                "CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON public.vector_store " +
                "USING hnsw (embedding vector_cosine_ops)"
            );
            log.info("成功创建向量索引");
            
        } catch (Exception e) {
            log.error("初始化 PGVector 失败: {}", e.getMessage());
            throw new RuntimeException("无法初始化 PGVector", e);
        }

        // 创建 PGVectorStore（initializeSchema 设为 false，因为我们已经手动创建了表）
        PgVectorStore vectorStore = PgVectorStore.builder(pgVectorJdbcTemplate(), embeddingModel)
                .dimensions(dimensions)
                .initializeSchema(false)  // 不自动建表，使用我们手动创建的表
                .build();

        // 检查是否已有数据，避免重复加载
        try {
            List<Document> existing = vectorStore.similaritySearch("test");
            if (!existing.isEmpty()) {
                log.info("PGVector 已有向量数据，跳过文档加载");
                return vectorStore;
            }
        } catch (Exception e) {
            log.warn("检查向量数据失败，将重新加载: {}", e.getMessage());
        }

        // 加载文档并写入向量存储
        log.info("开始加载文档到 PGVector");
        List<Document> documents = loveAppDocumentLoader.loadmarkdowns();

        if (documents.isEmpty()) {
            log.warn("没有文档被加载，向量存储为空");
        } else {
            log.info("准备添加 {} 个文档到 PGVector", documents.size());
            
            // 通义千问 embedding API 批次限制问题：一次只能处理少量文档
            // 为了避免超出限制，逐个添加文档
            for (int i = 0; i < documents.size(); i++) {
                vectorStore.add(List.of(documents.get(i)));
                if ((i + 1) % 10 == 0 || i == documents.size() - 1) {
                    log.info("已添加 {}/{} 个文档", i + 1, documents.size());
                }
            }
            
            log.info("文档添加完成，已持久化到 PostgreSQL");
        }

        return vectorStore;
    }
}
