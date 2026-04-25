package com.wly.ai_agent_plus.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoveApp 工具调用测试
 * 测试 AI Agent 使用各种工具的能力
 */
@SpringBootTest
@Slf4j
class LoveAppWithToolsTest {

    @Autowired
    private LoveApp loveApp;

    @Test
    void testChatWithTools_FileOperation() {
        log.info("\n========== 测试 1: 文件操作工具 ==========\n");
        
        String chatId = "test-file-operation-" + System.currentTimeMillis();
        String message = "请帮我创建一个文件，路径是 tmp/test.txt，内容是 'Hello from AI Agent'";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // 验证文件是否创建成功
        // 注意：这里只是测试 AI 是否能调用工具，实际文件创建由工具完成
    }

    @Test
    void testChatWithTools_WebScraping() {
        log.info("\n========== 测试 2: 网页抓取工具 ==========\n");
        
        String chatId = "test-web-scraping-" + System.currentTimeMillis();
        String message = "请帮我抓取 https://example.com 的网页内容，并告诉我网页的标题是什么";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够调用网页抓取工具并返回结果
        assertTrue(response.length() > 0);
    }

    @Test
    void testChatWithTools_ResourceDownload() {
        log.info("\n========== 测试 3: 资源下载工具 ==========\n");
        
        String chatId = "test-download-" + System.currentTimeMillis();
        String message = "请帮我下载 https://example.com 的网页，保存为 example-test.html";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够调用下载工具
        // 文件会保存到 tmp/download 目录
    }

    @Test
    void testChatWithTools_TerminalOperation() {
        log.info("\n========== 测试 4: 终端操作工具 ==========\n");
        
        String chatId = "test-terminal-" + System.currentTimeMillis();
        String message = "请帮我执行命令 'echo Hello World'，并告诉我输出结果";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够执行命令并返回结果
    }

    @Test
    void testChatWithTools_PDFGeneration() {
        log.info("\n========== 测试 5: PDF 生成工具 ==========\n");
        
        String chatId = "test-pdf-" + System.currentTimeMillis();
        String message = "请帮我生成一个 PDF 文档，文件名是 test-doc，内容是 '这是一个测试文档'";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够调用 PDF 生成工具
        // 文件会保存到 tmp/create 目录
    }

    @Test
    void testChatWithTools_WordGeneration() {
        log.info("\n========== 测试 6: Word 文档生成工具 ==========\n");
        
        String chatId = "test-word-" + System.currentTimeMillis();
        String message = "请帮我生成一个 Word 文档，文件名是 test-word，内容是 '这是一个测试 Word 文档'";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够调用 Word 生成工具
    }

    @Test
    void testChatWithTools_MultipleTools() {
        log.info("\n========== 测试 7: 多工具协作 ==========\n");
        
        String chatId = "test-multiple-tools-" + System.currentTimeMillis();
        String message = "请帮我：1. 抓取 https://example.com 的内容；2. 将内容保存为 PDF 文档，文件名是 example-content";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够依次调用多个工具完成任务
    }

    @Test
    void testChatWithTools_ConversationMemory() {
        log.info("\n========== 测试 8: 对话记忆 ==========\n");
        
        String chatId = "test-memory-" + System.currentTimeMillis();
        
        // 第一轮对话
        String message1 = "请帮我创建一个文件 tmp/memory-test.txt，内容是 'First message'";
        String response1 = loveApp.doChatWithTools(message1, chatId);
        assertNotNull(response1);
        log.info("第一轮 AI 回复: {}", response1);
        
        // 第二轮对话 - 测试是否记得之前的操作
        String message2 = "刚才我让你创建的文件路径是什么？";
        String response2 = loveApp.doChatWithTools(message2, chatId);
        assertNotNull(response2);
        log.info("第二轮 AI 回复: {}", response2);
        
        // AI 应该能够记住之前的对话内容
        assertTrue(response2.contains("tmp") || response2.contains("memory-test"));
    }

    @Test
    void testChatWithTools_NoToolNeeded() {
        log.info("\n========== 测试 9: 不需要工具的对话 ==========\n");
        
        String chatId = "test-no-tool-" + System.currentTimeMillis();
        String message = "你好，今天天气怎么样？";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够正常回复，即使不需要使用工具
        assertTrue(response.length() > 0);
    }

    @Test
    void testChatWithTools_ComplexTask() {
        log.info("\n========== 测试 10: 复杂任务 ==========\n");
        
        String chatId = "test-complex-" + System.currentTimeMillis();
        String message = """
                请帮我完成以下任务：
                1. 执行命令 'date' 获取当前日期
                2. 将日期信息保存为 PDF 文档，文件名是 current-date
                3. 告诉我文档保存在哪里
                """;
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够理解并执行复杂的多步骤任务
        assertTrue(response.length() > 0);
    }

    @Test
    void testChatWithTools_ErrorHandling() {
        log.info("\n========== 测试 11: 错误处理 ==========\n");
        
        String chatId = "test-error-" + System.currentTimeMillis();
        String message = "请帮我下载一个不存在的网址 https://this-url-does-not-exist-12345.com/file.txt";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够处理工具调用失败的情况
        // 并给出合理的错误提示
    }

    @Test
    void testChatWithTools_LoveAdviceWithTools() {
        log.info("\n========== 测试 12: 恋爱建议 + 工具使用 ==========\n");
        
        String chatId = "test-love-tools-" + System.currentTimeMillis();
        String message = "请给我一些恋爱建议，并将这些建议保存为 PDF 文档，文件名是 love-advice";
        
        String response = loveApp.doChatWithTools(message, chatId);
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        
        // AI 应该能够结合恋爱咨询和工具使用
        assertTrue(response.length() > 0);
    }

    @Test
    void testChatWithTools_ToolSelection() {
        log.info("\n========== 测试 13: 工具选择能力 ==========\n");
        
        String chatId = "test-tool-selection-" + System.currentTimeMillis();
        
        // 测试 AI 是否能正确选择合适的工具
        String[] messages = {
            "请帮我读取文件 README.md 的内容",  // 应该使用文件操作工具
            "请帮我下载 https://example.com",   // 应该使用下载工具
            "请帮我生成一个 PDF",                // 应该使用 PDF 生成工具
            "今天天气怎么样"                      // 不需要工具
        };
        
        for (String message : messages) {
            log.info("\n--- 测试消息: {} ---", message);
            String response = loveApp.doChatWithTools(message, chatId + "-" + System.currentTimeMillis());
            assertNotNull(response);
            log.info("AI 回复: {}", response);
        }
    }

    @Test
    void testChatWithTools_Performance() {
        log.info("\n========== 测试 14: 性能测试 ==========\n");
        
        String chatId = "test-performance-" + System.currentTimeMillis();
        String message = "你好";
        
        long startTime = System.currentTimeMillis();
        String response = loveApp.doChatWithTools(message, chatId);
        long duration = System.currentTimeMillis() - startTime;
        
        assertNotNull(response);
        log.info("AI 回复: {}", response);
        log.info("响应时间: {}ms", duration);
        
        // 简单对话应该在合理时间内完成（这里设置为 30 秒）
        assertTrue(duration < 30000, "响应时间过长: " + duration + "ms");
    }
}
