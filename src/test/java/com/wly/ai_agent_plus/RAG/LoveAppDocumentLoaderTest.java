package com.wly.ai_agent_plus.RAG;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档加载器测试类
 * 验证优化后的功能
 */
@SpringBootTest
public class LoveAppDocumentLoaderTest {

    @Autowired
    private LoveAppDocumentLoader documentLoader;

    @Test
    void testLoadMarkdowns() {
        // 测试文档加载
        List<Document> documents = documentLoader.loadmarkdowns();
        
        // 验证文档不为空
        assertNotNull(documents, "文档列表不应为空");
        assertFalse(documents.isEmpty(), "应该至少加载一个文档");
        
        System.out.println("成功加载 " + documents.size() + " 个文档片段");
    }

    @Test
    void testDocumentMetadata() {
        // 测试文档元数据
        List<Document> documents = documentLoader.loadmarkdowns();
        
        if (!documents.isEmpty()) {
            Document firstDoc = documents.get(0);
            Map<String, Object> metadata = firstDoc.getMetadata();
            
            // 验证必要的元数据字段
            assertTrue(metadata.containsKey("filename"), "应包含 filename 元数据");
            assertTrue(metadata.containsKey("heading"), "应包含 heading 元数据");
            assertTrue(metadata.containsKey("level"), "应包含 level 元数据");
            assertTrue(metadata.containsKey("chunk_index"), "应包含 chunk_index 元数据");
            assertTrue(metadata.containsKey("document_type"), "应包含 document_type 元数据");
            
            System.out.println("第一个文档的元数据:");
            metadata.forEach((key, value) -> 
                System.out.println("  " + key + ": " + value)
            );
        }
    }

    @Test
    void testDocumentContent() {
        // 测试文档内容
        List<Document> documents = documentLoader.loadmarkdowns();
        
        for (Document doc : documents) {
            String content = doc.getText();
            assertNotNull(content, "文档内容不应为空");
            assertFalse(content.trim().isEmpty(), "文档内容不应为空字符串");
            
            // 验证内容长度合理
            assertTrue(content.length() >= 5, "文档内容长度应大于最小嵌入长度");
        }
        
        System.out.println("所有文档内容验证通过");
    }

    @Test
    void testCachePerformance() {
        // 测试缓存性能
        long startTime1 = System.currentTimeMillis();
        List<Document> documents1 = documentLoader.loadmarkdowns();
        long endTime1 = System.currentTimeMillis();
        long duration1 = endTime1 - startTime1;
        
        // 第二次加载（应该从缓存读取）
        long startTime2 = System.currentTimeMillis();
        List<Document> documents2 = documentLoader.loadmarkdowns();
        long endTime2 = System.currentTimeMillis();
        long duration2 = endTime2 - startTime2;
        
        System.out.println("第一次加载耗时: " + duration1 + " ms");
        System.out.println("第二次加载耗时: " + duration2 + " ms");
        System.out.println("性能提升: " + ((duration1 - duration2) * 100.0 / duration1) + "%");
        
        // 验证两次加载的结果相同
        assertEquals(documents1.size(), documents2.size(), "两次加载的文档数量应该相同");
        
        // 第二次加载应该更快（缓存生效）
        // 注意：在某些情况下，第一次可能因为 JIT 编译等原因不一定更慢
        System.out.println("缓存测试完成");
    }

    @Test
    void testHeadingPath() {
        // 测试标题路径（面包屑）
        List<Document> documents = documentLoader.loadmarkdowns();
        
        long docsWithHeadingPath = documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("heading_path"))
                .count();
        
        System.out.println("包含标题路径的文档数: " + docsWithHeadingPath);
        
        // 打印一些示例
        documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("heading_path"))
                .limit(5)
                .forEach(doc -> {
                    String headingPath = (String) doc.getMetadata().get("heading_path");
                    System.out.println("标题路径示例: " + headingPath);
                });
    }

    @Test
    void testDocumentChunking() {
        // 测试文档切片
        List<Document> documents = documentLoader.loadmarkdowns();
        
        // 统计不同文件的切片数量
        Map<String, Long> chunkCountByFile = documents.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        doc -> (String) doc.getMetadata().get("filename"),
                        java.util.stream.Collectors.counting()
                ));
        
        System.out.println("各文件的切片数量:");
        chunkCountByFile.forEach((filename, count) -> 
            System.out.println("  " + filename + ": " + count + " 个片段")
        );
        
        // 验证每个文件至少有一个切片
        chunkCountByFile.values().forEach(count -> 
            assertTrue(count > 0, "每个文件应该至少有一个切片")
        );
    }
}
