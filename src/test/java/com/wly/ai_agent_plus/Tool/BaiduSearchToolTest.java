package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 百度搜索工具测试
 */
@SpringBootTest
class BaiduSearchToolTest {

    @Autowired
    private BaiduSearchTool baiduSearchTool;

    @Test
    void testBasicSearch() {
        // Given
        BaiduSearchTool.Request request = new BaiduSearchTool.Request("ChatGPT");
        
        // When
        BaiduSearchTool.Response response = baiduSearchTool.apply(request);
        
        // Then
        assertNotNull(response);
        if (response.error == null) {
            assertFalse(response.organicResults.isEmpty(), "应该有搜索结果");
            System.out.println("=== 搜索结果 ===");
            System.out.println(response.toFormattedString());
        } else {
            System.out.println("搜索失败: " + response.error);
        }
    }

    @Test
    void testChineseSearch() {
        // Given - 搜索中文内容
        BaiduSearchTool.Request request = new BaiduSearchTool.Request("人工智能", 1);
        
        // When
        BaiduSearchTool.Response response = baiduSearchTool.apply(request);
        
        // Then
        assertNotNull(response);
        System.out.println("=== 中文搜索结果 ===");
        System.out.println(response.toFormattedString());
    }

    @Test
    void testSearchWithAIAnswer() {
        // Given - 搜索可能触发 AI 回答的问题
        BaiduSearchTool.Request request = new BaiduSearchTool.Request("第一牛顿定律");
        
        // When
        BaiduSearchTool.Response response = baiduSearchTool.apply(request);
        
        // Then
        assertNotNull(response);
        System.out.println("=== AI 回答测试 ===");
        System.out.println(response.toFormattedString());
    }

    @Test
    void testSearchWithKnowledgeGraph() {
        // Given - 搜索可能触发知识图谱的实体
        BaiduSearchTool.Request request = new BaiduSearchTool.Request("New York");
        
        // When
        BaiduSearchTool.Response response = baiduSearchTool.apply(request);
        
        // Then
        assertNotNull(response);
        System.out.println("=== 知识图谱测试 ===");
        System.out.println(response.toFormattedString());
    }
}
