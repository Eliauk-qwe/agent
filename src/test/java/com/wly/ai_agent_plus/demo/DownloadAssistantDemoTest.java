package com.wly.ai_agent_plus.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * 下载助手示例测试
 */
@SpringBootTest
class DownloadAssistantDemoTest {

    @Autowired
    private DownloadAssistantDemo demo;

    @Test
    void testDownloadImagesFromPage() {
        System.out.println("\n========== 示例 1: 下载网页图片 ==========\n");
        
        String result = demo.downloadImagesFromPage(
            "https://example.com",
            "./downloads/images"
        );
        
        System.out.println(result);
    }

    @Test
    void testBatchDownload() {
        System.out.println("\n========== 示例 2: 批量下载 ==========\n");
        
        List<String> urls = Arrays.asList(
            "https://example.com",
            "https://example.org",
            "https://example.net"
        );
        
        String result = demo.downloadList(urls, "./downloads/batch");
        
        System.out.println(result);
    }

    @Test
    void testDownloadAndAnalyze() {
        System.out.println("\n========== 示例 3: 下载并分析 ==========\n");
        
        String result = demo.downloadAndAnalyze("https://example.com");
        
        System.out.println(result);
    }

    @Test
    void testDownloadIfNotExists() {
        System.out.println("\n========== 示例 4: 检查并下载 ==========\n");
        
        String savePath = "./downloads/check-test.html";
        
        // 第一次：文件不存在，会下载
        System.out.println("第一次调用:");
        String result1 = demo.downloadIfNotExists("https://example.com", savePath);
        System.out.println(result1);
        
        System.out.println("\n第二次调用:");
        // 第二次：文件已存在，不会下载
        String result2 = demo.downloadIfNotExists("https://example.com", savePath);
        System.out.println(result2);
    }
}
