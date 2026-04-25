package com.wly.ai_agent_plus.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 简单网页分析器测试
 */
@SpringBootTest
class SimpleWebAnalyzerTest {

    @Autowired
    private SimpleWebAnalyzer analyzer;

    // 测试用的 LeetCode 文章 URL
    private static final String TEST_URL = "https://leetcode.cn/discuss/post/3141566/ru-he-ke-xue-shua-ti-by-endlesscheng-q3yd/";

    @Test
    void testAnalyzeWebPage() {
        System.out.println("\n========== 测试 1: 分析网页（通用） ==========\n");
        
        String result = analyzer.analyzeWebPage(TEST_URL);
        
        System.out.println("【AI 分析】");
        System.out.println(result);
    }

    @Test
    void testAnalyzeWithQuestion() {
        System.out.println("\n========== 测试 2: 分析网页（带问题） ==========\n");
        
        String result = analyzer.analyzeWebPage(
            TEST_URL,
            "这篇文章主要讲了什么刷题方法？"
        );
        
        System.out.println("【AI 回答】");
        System.out.println(result);
    }

    @Test
    void testMultipleQuestions() {
        System.out.println("\n========== 测试 3: 多个问题分析 ==========\n");
        
        String[] questions = {
            "文章的核心观点是什么？",
            "作者推荐的刷题顺序是什么？",
            "有哪些具体的学习建议？"
        };
        
        for (int i = 0; i < questions.length; i++) {
            System.out.println("\n--- 问题 " + (i + 1) + " ---");
            System.out.println("问题: " + questions[i]);
            
            String result = analyzer.analyzeWebPage(TEST_URL, questions[i]);
            System.out.println("\n回答:");
            System.out.println(result);
        }
    }
}
