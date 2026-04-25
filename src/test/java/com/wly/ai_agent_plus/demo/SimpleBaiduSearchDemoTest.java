package com.wly.ai_agent_plus.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 简单百度搜索示例测试
 * 
 * 这些示例不需要配置 Function Calling，更容易上手
 */
@SpringBootTest
class SimpleBaiduSearchDemoTest {

    @Autowired
    private SimpleBaiduSearchDemo demo;

    @Test
    void testSearchAndAnswer() {
        System.out.println("\n========== 示例 1: 搜索 + AI 回答 ==========\n");
        
        String answer = demo.searchAndAnswer("什么是 Spring AI？");
        
        System.out.println("【AI 回答】");
        System.out.println(answer);
    }

    @Test
    void testGetLoveAdviceWithSearch() {
        System.out.println("\n========== 示例 2: 恋爱建议 + 实时搜索 ==========\n");
        
        String advice = demo.getLoveAdviceWithSearch("异地恋如何维持");
        
        System.out.println("【恋爱建议】");
        System.out.println(advice);
    }

    @Test
    void testCompareTopics() {
        System.out.println("\n========== 示例 3: 对比分析 ==========\n");
        
        String comparison = demo.compareTopics("ChatGPT", "文心一言");
        
        System.out.println("【对比分析】");
        System.out.println(comparison);
    }

    @Test
    void testGetLatestTrends() {
        System.out.println("\n========== 示例 4: 获取最新趋势 ==========\n");
        
        String trends = demo.getLatestTrends("人工智能");
        
        System.out.println("【最新趋势】");
        System.out.println(trends);
    }

    @Test
    void testQuickFactCheck() {
        System.out.println("\n========== 示例 5: 快速事实查询 ==========\n");
        
        String[] questions = {
            "第一牛顿定律是什么",
            "北京的人口是多少",
            "ChatGPT 是什么时候发布的"
        };
        
        for (String question : questions) {
            System.out.println("\n问题: " + question);
            String answer = demo.quickFactCheck(question);
            System.out.println("回答: " + answer);
            System.out.println("---");
        }
    }

    @Test
    void testLoveTopics() {
        System.out.println("\n========== 示例 6: 多个恋爱话题搜索 ==========\n");
        
        String[] topics = {
            "如何提升恋爱中的沟通能力",
            "婚后如何保持新鲜感",
            "单身如何提升自己的魅力"
        };
        
        for (String topic : topics) {
            System.out.println("\n【话题】" + topic);
            String advice = demo.getLoveAdviceWithSearch(topic);
            System.out.println(advice);
            System.out.println("\n" + "=".repeat(50) + "\n");
        }
    }
}
