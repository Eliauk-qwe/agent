package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网页抓取工具测试
 */
@SpringBootTest
class WebScraperToolTest {

    @Autowired
    private WebScraperTool webScraperTool;

    // 测试用的 LeetCode 文章 URL
    private static final String TEST_URL = "https://leetcode.cn/discuss/post/3141566/ru-he-ke-xue-shua-ti-by-endlesscheng-q3yd/";

    @Test
    void testBasicScraping() {
        System.out.println("\n========== 测试 1: 基础抓取 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(TEST_URL);
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success, "抓取应该成功");
        assertNotNull(response.title, "应该有标题");
        assertNotNull(response.content, "应该有内容");
        assertFalse(response.content.isEmpty(), "内容不应为空");
        
        System.out.println("标题: " + response.title);
        System.out.println("内容长度: " + response.content.length() + " 字符");
        System.out.println("内容预览: " + response.content.substring(0, Math.min(300, response.content.length())));
    }

    @Test
    void testCleanTextMode() {
        System.out.println("\n========== 测试 2: 干净文本模式 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(
            TEST_URL,
            WebScraperTool.ExtractMode.CLEAN_TEXT
        );
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success);
        System.out.println("标题: " + response.title);
        System.out.println("内容长度: " + response.content.length());
        System.out.println("内容预览: " + response.content.substring(0, Math.min(500, response.content.length())));
    }

    @Test
    void testMainContentMode() {
        System.out.println("\n========== 测试 3: 主要内容模式 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(
            TEST_URL,
            WebScraperTool.ExtractMode.MAIN_CONTENT
        );
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success);
        System.out.println("标题: " + response.title);
        System.out.println("内容长度: " + response.content.length());
        System.out.println("内容预览: " + response.content.substring(0, Math.min(500, response.content.length())));
    }

    @Test
    void testExtractLinks() {
        System.out.println("\n========== 测试 4: 提取链接 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(TEST_URL);
        request.extractLinks = true;
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success);
        assertNotNull(response.links);
        
        System.out.println("找到 " + response.links.size() + " 个链接");
        if (!response.links.isEmpty()) {
            System.out.println("\n前 10 个链接:");
            for (int i = 0; i < Math.min(10, response.links.size()); i++) {
                System.out.println((i + 1) + ". " + response.links.get(i));
            }
        }
    }

    @Test
    void testExtractImages() {
        System.out.println("\n========== 测试 5: 提取图片 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(TEST_URL);
        request.extractImages = true;
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success);
        assertNotNull(response.images);
        
        System.out.println("找到 " + response.images.size() + " 张图片");
        if (!response.images.isEmpty()) {
            System.out.println("\n图片列表:");
            for (int i = 0; i < Math.min(5, response.images.size()); i++) {
                System.out.println((i + 1) + ". " + response.images.get(i));
            }
        }
    }

    @Test
    void testExtractLinksAndImages() {
        System.out.println("\n========== 测试 6: 同时提取链接和图片 ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(TEST_URL);
        request.extractLinks = true;
        request.extractImages = true;
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertTrue(response.success);
        
        System.out.println("标题: " + response.title);
        System.out.println("链接数量: " + response.links.size());
        System.out.println("图片数量: " + response.images.size());
        System.out.println("\n" + response.toFormattedString());
    }

    @Test
    void testInvalidUrl() {
        System.out.println("\n========== 测试 7: 无效 URL ==========\n");
        
        WebScraperTool.Request request = new WebScraperTool.Request(
            "https://this-url-does-not-exist-12345.com"
        );
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        assertFalse(response.success, "无效 URL 应该失败");
        assertNotNull(response.error, "应该有错误信息");
        
        System.out.println("错误信息: " + response.error);
    }
}
