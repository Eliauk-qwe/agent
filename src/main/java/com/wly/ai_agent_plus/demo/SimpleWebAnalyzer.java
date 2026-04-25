package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.WebScraperTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 简单网页分析器
 * 
 * 功能：给一个网址，读取内容并让 AI 分析
 */
@Component
@Slf4j
public class SimpleWebAnalyzer {

    @Autowired
    private WebScraperTool webScraperTool;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 分析网页内容
     * 
     * @param url 网页地址
     * @param question 用户问题（可选，如果为空则做通用分析）
     * @return AI 分析结果
     */
    public String analyzeWebPage(String url, String question) {
        log.info("分析网页: {}", url);
        
        // 第一步：抓取网页内容
        WebScraperTool.Request request = new WebScraperTool.Request(
            url,
            WebScraperTool.ExtractMode.MAIN_CONTENT
        );
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        if (!response.success) {
            return "网页抓取失败: " + response.error;
        }
        
        log.info("抓取成功，内容长度: {} 字符", response.content.length());
        
        // 第二步：AI 分析
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt;
        if (question != null && !question.isEmpty()) {
            // 用户有具体问题
            prompt = String.format("""
                    网页标题：%s
                    网页地址：%s
                    
                    网页内容：
                    %s
                    
                    用户问题：%s
                    
                    请基于网页内容回答用户的问题。
                    """, 
                    response.title,
                    url,
                    limitContent(response.content, 3000),
                    question);
        } else {
            // 通用分析
            prompt = String.format("""
                    网页标题：%s
                    网页地址：%s
                    
                    网页内容：
                    %s
                    
                    请分析这个网页的主要内容，包括：
                    1. 主题是什么
                    2. 主要讲了什么
                    3. 关键信息有哪些
                    """, 
                    response.title,
                    url,
                    limitContent(response.content, 3000));
        }
        
        String analysis = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("分析完成");
        return analysis;
    }

    /**
     * 分析网页（无问题，做通用分析）
     */
    public String analyzeWebPage(String url) {
        return analyzeWebPage(url, null);
    }

    /**
     * 限制内容长度
     */
    private String limitContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "\n...(内容过长，已截断)";
    }
}
