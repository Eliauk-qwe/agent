package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.BaiduSearchTool;
import com.wly.ai_agent_plus.Tool.WebScraperTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 搜索 + 抓取组合示例
 * 
 * 演示如何结合使用：
 *   1. 百度搜索 - 找到相关网页
 *   2. 网页抓取 - 获取完整内容
 *   3. AI 总结 - 生成答案
 */
@Component
@Slf4j
public class SearchAndScrapDemo {

    @Autowired
    private BaiduSearchTool baiduSearchTool;

    @Autowired
    private WebScraperTool webScraperTool;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 示例 1: 搜索 → 抓取 → 总结
     * 
     * 完整流程：
     *   1. 用百度搜索找到相关网页
     *   2. 让 AI 从搜索结果中选择最相关的
     *   3. 抓取选中的网页内容
     *   4. 让 AI 基于完整内容回答问题
     */
    public String searchScrapAndAnswer(String question) {
        log.info("=== 搜索 → 抓取 → 总结 ===");
        log.info("问题: {}", question);
        
        // 第一步：百度搜索
        log.info("步骤 1: 百度搜索...");
        BaiduSearchTool.Request searchRequest = new BaiduSearchTool.Request(question);
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(searchRequest);
        
        if (searchResponse.error != null || searchResponse.organicResults.isEmpty()) {
            return "搜索失败或没有结果";
        }
        
        // 第二步：让 AI 选择最相关的搜索结果
        log.info("步骤 2: AI 选择最相关的结果...");
        ChatClient chatClient = chatClientBuilder.build();
        
        StringBuilder searchResultsSummary = new StringBuilder();
        for (int i = 0; i < Math.min(5, searchResponse.organicResults.size()); i++) {
            BaiduSearchTool.OrganicResult result = searchResponse.organicResults.get(i);
            searchResultsSummary.append(String.format("%d. %s\n   摘要: %s\n   链接: %s\n\n", 
                i + 1, result.title, result.snippet, result.link));
        }
        
        String selectionPrompt = String.format("""
                用户问题：%s
                
                搜索结果：
                %s
                
                请选择最相关的一个结果（只返回数字 1-5），如果都不相关返回 0。
                """, question, searchResultsSummary.toString());
        
        String selection = chatClient.prompt()
                .user(selectionPrompt)
                .call()
                .content()
                .trim();
        
        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(selection) - 1;
            if (selectedIndex < 0 || selectedIndex >= searchResponse.organicResults.size()) {
                return "搜索结果都不相关，无法回答该问题";
            }
        } catch (NumberFormatException e) {
            // 如果 AI 没有返回数字，默认选择第一个
            selectedIndex = 0;
        }
        
        BaiduSearchTool.OrganicResult selectedResult = searchResponse.organicResults.get(selectedIndex);
        log.info("AI 选择了第 {} 个结果: {}", selectedIndex + 1, selectedResult.title);
        
        // 第三步：抓取选中的网页内容
        log.info("步骤 3: 抓取网页内容...");
        WebScraperTool.Request scrapRequest = new WebScraperTool.Request(
            selectedResult.link,
            WebScraperTool.ExtractMode.MAIN_CONTENT
        );
        WebScraperTool.Response scrapResponse = webScraperTool.apply(scrapRequest);
        
        if (!scrapResponse.success) {
            return "网页抓取失败: " + scrapResponse.error;
        }
        
        log.info("抓取成功，内容长度: {} 字符", scrapResponse.content.length());
        
        // 第四步：AI 总结
        log.info("步骤 4: AI 总结...");
        
        String prompt = String.format("""
                用户问题：%s
                
                我从网页"%s"抓取了以下内容：
                
                %s
                
                请基于以上内容，用简洁易懂的语言回答用户的问题。
                如果内容与问题不相关，请明确说明，并提供你自己的建议。
                """, question, selectedResult.title, 
                // 限制内容长度，避免超过 token 限制
                scrapResponse.content.length() > 3000 
                    ? scrapResponse.content.substring(0, 3000) + "..." 
                    : scrapResponse.content);
        
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("总结完成");
        return answer;
    }

    /**
     * 示例 2: 深度研究
     * 
     * 抓取多个搜索结果，综合分析
     */
    public String deepResearch(String topic, int maxPages) {
        log.info("=== 深度研究: {} ===", topic);
        
        // 第一步：搜索
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
            new BaiduSearchTool.Request(topic)
        );
        
        if (searchResponse.error != null || searchResponse.organicResults.isEmpty()) {
            return "搜索失败";
        }
        
        // 第二步：抓取前 N 个结果
        StringBuilder allContent = new StringBuilder();
        int successCount = 0;
        
        for (int i = 0; i < Math.min(maxPages, searchResponse.organicResults.size()); i++) {
            BaiduSearchTool.OrganicResult result = searchResponse.organicResults.get(i);
            log.info("抓取第 {} 个网页: {}", i + 1, result.title);
            
            WebScraperTool.Response scrapResponse = webScraperTool.apply(
                new WebScraperTool.Request(result.link, WebScraperTool.ExtractMode.MAIN_CONTENT)
            );
            
            if (scrapResponse.success) {
                allContent.append("\n\n【来源 ").append(i + 1).append(": ").append(result.title).append("】\n");
                // 每个页面最多取 1000 字符
                String content = scrapResponse.content.length() > 1000 
                    ? scrapResponse.content.substring(0, 1000) 
                    : scrapResponse.content;
                allContent.append(content);
                successCount++;
            }
        }
        
        log.info("成功抓取 {} 个网页", successCount);
        
        // 第三步：综合分析
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                请综合分析以下 %d 个网页的内容，总结关于"%s"的关键信息：
                
                %s
                
                要求：
                1. 提取最重要的信息
                2. 综合不同来源的观点
                3. 用结构化的方式呈现
                """, successCount, topic, allContent.toString());
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 3: 对比分析（搜索 + 抓取）
     */
    public String compareWithFullContent(String topic1, String topic2) {
        log.info("=== 对比分析（完整内容）: {} vs {} ===", topic1, topic2);
        
        // 搜索并抓取第一个话题
        String content1 = searchAndScrap(topic1);
        
        // 搜索并抓取第二个话题
        String content2 = searchAndScrap(topic2);
        
        if (content1 == null || content2 == null) {
            return "抓取失败";
        }
        
        // AI 对比
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
                """, topic1, content1, topic2, content2);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 辅助方法：搜索并抓取第一个结果
     */
    private String searchAndScrap(String query) {
        // 搜索
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
            new BaiduSearchTool.Request(query)
        );
        
        if (searchResponse.error != null || searchResponse.organicResults.isEmpty()) {
            return null;
        }
        
        // 抓取
        String url = searchResponse.organicResults.get(0).link;
        WebScraperTool.Response scrapResponse = webScraperTool.apply(
            new WebScraperTool.Request(url, WebScraperTool.ExtractMode.MAIN_CONTENT)
        );
        
        if (!scrapResponse.success) {
            return null;
        }
        
        // 限制长度
        return scrapResponse.content.length() > 2000 
            ? scrapResponse.content.substring(0, 2000) 
            : scrapResponse.content;
    }

    /**
     * 示例 4: 提取特定信息
     * 
     * 使用自定义选择器提取特定内容
     */
    public String extractSpecificInfo(String url, String cssSelector, String question) {
        log.info("=== 提取特定信息 ===");
        log.info("URL: {}", url);
        log.info("选择器: {}", cssSelector);
        
        // 抓取特定内容
        WebScraperTool.Request request = new WebScraperTool.Request(url);
        request.extractMode = WebScraperTool.ExtractMode.CUSTOM;
        request.cssSelector = cssSelector;
        
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        if (!response.success) {
            return "抓取失败: " + response.error;
        }
        
        // AI 处理
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                从网页中提取的内容：
                
                %s
                
                用户问题：%s
                
                请基于提取的内容回答问题。
                """, response.content, question);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
