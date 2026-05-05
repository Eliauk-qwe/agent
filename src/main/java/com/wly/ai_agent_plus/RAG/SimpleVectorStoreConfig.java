package com.wly.ai_agent_plus.RAG;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

/**
 * SimpleVectorStore 配置类 - 内存向量存储
 * 
 * 特点：
 * - 向量存储在 JVM 内存中
 * - 应用重启后数据丢失
 * - 每次启动都需要重新加载文档并调用 embedding API
 * - 适合开发测试环境
 * 
 * 调用关系：
 *   应用启动
 *     → loveAppVectorStore Bean 初始化
 *       → 创建 SimpleVectorStore
 *       → 调用 LoveAppDocumentLoader 加载文档
 *       → 调用 embedding API 计算向量（36 次）
 *       → 存储到内存
 *     → 应用重启：重复上述过程
 * 
 * 如需持久化向量存储，请使用 LoveAppVectorStoreConfig（PGVector）
 */
@Configuration
@Slf4j
public class SimpleVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Value("${spring.ai.vectorstore.persist.enabled:true}")
    private boolean persistEnabled;

    @Value("${spring.ai.vectorstore.persist.path:./data/vectorstore.json}")
    private String persistPath;

    /**
     * 创建 SimpleVectorStore Bean
     * 
     * 注意：每次应用启动都会重新加载文档并调用 embedding API
     */
    @Bean
    public VectorStore loveAppVectorStore(EmbeddingModel embeddingModel) {
        log.info("初始化 SimpleVectorStore（内存向量存储）");
        
        // 创建 SimpleVectorStore - 使用 builder 模式
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        File storeFile = new File(persistPath);
        if (persistEnabled && storeFile.exists() && storeFile.length() > 0) {
            log.info("从本地文件加载 SimpleVectorStore: {}", storeFile.getAbsolutePath());
            vectorStore.load(storeFile);
            return vectorStore;
        }
        
        // 加载文档
        log.info("开始加载文档到 SimpleVectorStore");
        List<Document> documents = loveAppDocumentLoader.loadmarkdowns();
        
        if (documents.isEmpty()) {
            log.warn("没有文档被加载，向量存储为空");
        } else {
            log.info("准备添加 {} 个文档到 SimpleVectorStore", documents.size());
            
            // 添加文档到向量存储（会调用 embedding API）
            vectorStore.add(documents);

            if (persistEnabled) {
                File parentDir = storeFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    log.info("创建向量存储目录: {}, 结果: {}", parentDir.getAbsolutePath(), created);
                }
                vectorStore.save(storeFile);
                log.info("SimpleVectorStore 已保存到本地文件: {}", storeFile.getAbsolutePath());
            }
            
            log.info("文档添加完成，已存储到内存（应用重启后会丢失）");
        }
        
        return vectorStore;
    }
}
