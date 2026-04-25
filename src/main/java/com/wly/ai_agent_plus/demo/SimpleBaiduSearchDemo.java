package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.BaiduSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 百度搜索工具简单示例
 * 
 * 不使用 Function Calling，直接调用搜索工具并让 AI 总结
 * 这种方式更简单，适合快速上手
 */
@Component
@Slf4j
public class SimpleBaiduSearchDemo {

    @Autowired
    private BaiduSearchTool baiduSearchTool;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 示例 1: 搜索 + AI 总结
     */
    public String searchAndAnswer(String question) {
        log.info("用户问题: {}", question);
        
        // 1. 先用百度搜索
        BaiduSearchTool.Request searchRequest = new BaiduSearchTool.Request(question);
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(searchRequest);
        
        if (searchResponse.error != null) {
            return "搜索失败: " + searchResponse.error;
        }
        
        // 2. 让 AI 基于搜索结果回答
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                用户问题：%s
                
                我已经为你搜索了相关信息：
                
                %s
                
                请基于以上搜索结果，用简洁易懂的语言回答用户的问题。
                如果搜索结果中有 AI 回答或知识图谱，优先使用这些信息。
                """, question, searchResponse.toFormattedString());
        
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("AI 回答: {}", answer);
        return answer;
    }

    /**
     * 示例 2: 恋爱建议 + 实时搜索
     */
    public String getLoveAdviceWithSearch(String topic) {
        log.info("恋爱话题: {}", topic);
        
        // 1. 搜索相关恋爱建议
        BaiduSearchTool.Request searchRequest = new BaiduSearchTool.Request(topic + " 恋爱建议");
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(searchRequest);
        
        if (searchResponse.error != null) {
            return "搜索失败: " + searchResponse.error;
        }
        
        // 2. 让恋爱助手基于搜索结果给建议
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("""
                    你是一位温暖、专业的恋爱顾问。
                    你的回答要：
                    1. 温柔体贴，充满同理心
                    2. 实用可行，有具体建议
                    3. 结合最新的恋爱趋势和心理学知识
                    """)
                .build();
        
        String prompt = String.format("""
                用户想了解：%s
                
                我为你搜索了最新的相关信息：
                
                %s
                
                请基于这些信息，给用户一些温暖、实用的恋爱建议。
                """, topic, searchResponse.toFormattedString());
        
        String advice = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("恋爱建议: {}", advice);
        return advice;
    }

    /**
     * 示例 3: 对比分析
     */
    public String compareTopics(String topic1, String topic2) {
        log.info("对比: {} vs {}", topic1, topic2);
        
        // 搜索两个话题
        BaiduSearchTool.Response response1 = baiduSearchTool.apply(
                new BaiduSearchTool.Request(topic1));
        BaiduSearchTool.Response response2 = baiduSearchTool.apply(
                new BaiduSearchTool.Request(topic2));
        
        if (response1.error != null || response2.error != null) {
            return "搜索失败";
        }
        
        // AI 对比分析
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                请对比分析以下两个话题：
                
                【话题 1: %s】
                %s
                
                【话题 2: %s】
                %s
                
                请从以下角度分析：
                1. 主要特点和差异
                2. 各自的优势和劣势
                3. 适用场景
                4. 给出建议
                """, topic1, response1.toFormattedString(),
                     topic2, response2.toFormattedString());
        
        String comparison = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("对比分析完成");
        return comparison;
    }

    /**
     * 示例 4: 获取最新趋势
     */
    public String getLatestTrends(String domain) {
        log.info("获取最新趋势: {}", domain);
        
        // 搜索最新趋势
        String query = domain + " 2026 最新趋势";
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
                new BaiduSearchTool.Request(query));
        
        if (searchResponse.error != null) {
            return "搜索失败: " + searchResponse.error;
        }
        
        // AI 总结趋势
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                请总结 %s 领域的最新趋势：
                
                %s
                
                要求：
                1. 提取 3-5 个关键趋势
                2. 每个趋势用简短的语言描述
                3. 说明这些趋势的影响和意义
                """, domain, searchResponse.toFormattedString());
        
        String trends = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("趋势总结完成");
        return trends;
    }

    /**
     * 示例 5: 快速事实查询
     */
    public String quickFactCheck(String question) {
        log.info("快速查询: {}", question);
        
        // 搜索
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
                new BaiduSearchTool.Request(question));
        
        if (searchResponse.error != null) {
            return "搜索失败: " + searchResponse.error;
        }
        
        // 如果有 AI 回答或知识图谱，直接返回
        if (searchResponse.answerBox != null && searchResponse.answerBox.answer != null) {
            return "【AI 回答】\n" + searchResponse.answerBox.answer;
        }
        
        if (searchResponse.knowledgeGraph != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("【知识图谱】\n");
            sb.append(searchResponse.knowledgeGraph.title).append("\n");
            if (searchResponse.knowledgeGraph.description != null) {
                sb.append(searchResponse.knowledgeGraph.description);
            }
            return sb.toString();
        }
        
        // 否则让 AI 简短回答
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                问题：%s
                
                搜索结果：
                %s
                
                请用 1-2 句话简短回答这个问题。
                """, question, searchResponse.toFormattedString());
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
