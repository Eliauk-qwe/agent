package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.BaiduSearchTool;
import com.wly.ai_agent_plus.Tool.FileDownloadTool;
import com.wly.ai_agent_plus.Tool.WebScraperTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载助手示例
 * 
 * 演示如何结合搜索、抓取和下载功能
 */
@Component
@Slf4j
public class DownloadAssistantDemo {

    @Autowired
    private FileDownloadTool fileDownloadTool;

    @Autowired
    private BaiduSearchTool baiduSearchTool;

    @Autowired
    private WebScraperTool webScraperTool;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 示例 1: 搜索并下载资源
     * 
     * 根据关键词搜索，找到资源链接，然后下载
     */
    public String searchAndDownload(String keyword, String fileType) {
        log.info("=== 搜索并下载: {} ===", keyword);
        
        // 第一步：搜索
        String searchQuery = keyword + " " + fileType + " 下载";
        BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
            new BaiduSearchTool.Request(searchQuery)
        );
        
        if (searchResponse.error != null || searchResponse.organicResults.isEmpty()) {
            return "搜索失败";
        }
        
        // 第二步：从搜索结果中提取下载链接
        List<String> downloadLinks = new ArrayList<>();
        for (BaiduSearchTool.OrganicResult result : searchResponse.organicResults) {
            // 简单判断：如果链接包含文件扩展名，可能是下载链接
            if (result.link.contains("." + fileType)) {
                downloadLinks.add(result.link);
            }
        }
        
        if (downloadLinks.isEmpty()) {
            return "未找到可下载的资源";
        }
        
        // 第三步：下载第一个找到的资源
        String downloadUrl = downloadLinks.get(0);
        log.info("找到下载链接: {}", downloadUrl);
        
        FileDownloadTool.Request downloadRequest = FileDownloadTool.Downloads.toDirectory(
            downloadUrl,
            "./downloads/" + keyword
        );
        
        FileDownloadTool.Response downloadResponse = fileDownloadTool.apply(downloadRequest);
        
        if (downloadResponse.success) {
            return String.format("下载成功！\n%s", downloadResponse.toFormattedString());
        } else {
            return "下载失败: " + downloadResponse.error;
        }
    }

    /**
     * 示例 2: 从网页中提取并下载所有图片
     */
    public String downloadImagesFromPage(String url, String saveDirectory) {
        log.info("=== 下载网页图片: {} ===", url);
        
        // 第一步：抓取网页，提取图片链接
        WebScraperTool.Request scrapRequest = new WebScraperTool.Request(url);
        scrapRequest.extractImages = true;
        
        WebScraperTool.Response scrapResponse = webScraperTool.apply(scrapRequest);
        
        if (!scrapResponse.success || scrapResponse.images == null || scrapResponse.images.isEmpty()) {
            return "未找到图片";
        }
        
        log.info("找到 {} 张图片", scrapResponse.images.size());
        
        // 第二步：下载所有图片
        List<FileDownloadTool.Request> downloadRequests = new ArrayList<>();
        for (int i = 0; i < Math.min(10, scrapResponse.images.size()); i++) {
            String imageUrl = scrapResponse.images.get(i);
            FileDownloadTool.Request request = FileDownloadTool.Downloads.toDirectory(
                imageUrl,
                saveDirectory
            );
            downloadRequests.add(request);
        }
        
        FileDownloadTool.Response[] responses = fileDownloadTool.downloadBatch(
            downloadRequests.toArray(new FileDownloadTool.Request[0])
        );
        
        // 统计结果
        int successCount = 0;
        long totalSize = 0;
        for (FileDownloadTool.Response response : responses) {
            if (response.success) {
                successCount++;
                totalSize += response.fileSize;
            }
        }
        
        return String.format("下载完成！\n成功: %d/%d\n总大小: %.2f MB", 
                successCount, responses.length, totalSize / (1024.0 * 1024));
    }

    /**
     * 示例 3: 智能下载助手
     * 
     * AI 理解用户意图，自动搜索和下载
     */
    public String smartDownload(String userIntent) {
        log.info("=== 智能下载: {} ===", userIntent);
        
        // 第一步：让 AI 理解用户意图
        ChatClient chatClient = chatClientBuilder.build();
        
        String analysisPrompt = String.format("""
                用户想要：%s
                
                请分析：
                1. 用户想下载什么类型的文件？（如：PDF、图片、视频等）
                2. 搜索关键词应该是什么？
                
                只返回 JSON 格式：
                {"fileType": "文件类型", "keyword": "搜索关键词"}
                """, userIntent);
        
        String analysis = chatClient.prompt()
                .user(analysisPrompt)
                .call()
                .content();
        
        log.info("AI 分析: {}", analysis);
        
        // 简单解析（实际应该用 JSON 解析）
        // 这里为了演示，直接搜索用户的原始意图
        return searchAndDownload(userIntent, "pdf");
    }

    /**
     * 示例 4: 批量下载列表
     */
    public String downloadList(List<String> urls, String saveDirectory) {
        log.info("=== 批量下载 {} 个文件 ===", urls.size());
        
        List<FileDownloadTool.Request> requests = new ArrayList<>();
        for (String url : urls) {
            requests.add(FileDownloadTool.Downloads.toDirectory(url, saveDirectory));
        }
        
        FileDownloadTool.Response[] responses = fileDownloadTool.downloadBatch(
            requests.toArray(new FileDownloadTool.Request[0])
        );
        
        // 生成报告
        StringBuilder report = new StringBuilder();
        report.append("【批量下载报告】\n\n");
        
        int successCount = 0;
        long totalSize = 0;
        long totalTime = 0;
        
        for (int i = 0; i < responses.length; i++) {
            FileDownloadTool.Response response = responses[i];
            report.append(String.format("%d. %s\n", i + 1, 
                    response.success ? "✅ 成功" : "❌ 失败"));
            
            if (response.success) {
                successCount++;
                totalSize += response.fileSize;
                totalTime += response.downloadTimeMs;
                report.append(String.format("   文件: %s (%.2f KB)\n", 
                        response.fileName, response.fileSize / 1024.0));
            } else {
                report.append(String.format("   错误: %s\n", response.error));
            }
        }
        
        report.append(String.format("\n总计: %d/%d 成功\n", successCount, responses.length));
        report.append(String.format("总大小: %.2f MB\n", totalSize / (1024.0 * 1024)));
        report.append(String.format("总耗时: %.2f 秒\n", totalTime / 1000.0));
        
        return report.toString();
    }

    /**
     * 示例 5: 下载并分析文件
     */
    public String downloadAndAnalyze(String url) {
        log.info("=== 下载并分析: {} ===", url);
        
        // 下载文件
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toDefaultDir(url);
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        if (!response.success) {
            return "下载失败: " + response.error;
        }
        
        // 获取文件信息
        FileDownloadTool.FileInfo info = fileDownloadTool.getFileInfo(response.savePath);
        
        // AI 分析
        ChatClient chatClient = chatClientBuilder.build();
        
        String analysisPrompt = String.format("""
                已下载文件：
                - 文件名: %s
                - 大小: %.2f KB
                - 下载速度: %.2f KB/s
                - 保存路径: %s
                
                请分析这个文件的基本信息，并给出使用建议。
                """, 
                response.fileName,
                response.fileSize / 1024.0,
                response.speedKBps,
                response.savePath);
        
        String analysis = chatClient.prompt()
                .user(analysisPrompt)
                .call()
                .content();
        
        return String.format("%s\n\n【AI 分析】\n%s", 
                response.toFormattedString(), analysis);
    }

    /**
     * 示例 6: 检查并下载（如果不存在）
     */
    public String downloadIfNotExists(String url, String savePath) {
        log.info("=== 检查并下载: {} ===", url);
        
        // 检查文件是否已存在
        if (fileDownloadTool.fileExists(savePath)) {
            FileDownloadTool.FileInfo info = fileDownloadTool.getFileInfo(savePath);
            return String.format("文件已存在，无需下载\n%s", info);
        }
        
        // 文件不存在，开始下载
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toPath(url, savePath);
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        if (response.success) {
            return String.format("文件不存在，已下载\n%s", response.toFormattedString());
        } else {
            return "下载失败: " + response.error;
        }
    }
}
