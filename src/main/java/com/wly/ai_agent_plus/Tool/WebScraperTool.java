package com.wly.ai_agent_plus.Tool;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 网页抓取工具
 * 
 * 使用 Jsoup 实现网页内容抓取和解析
 * 
 * 功能：
 *   - 抓取网页完整内容
 *   - 提取标题、正文、链接等
 *   - 清洗 HTML，提取纯文本
 *   - 支持自定义 User-Agent
 */
@Component
@Slf4j
public class WebScraperTool implements Function<WebScraperTool.Request, WebScraperTool.Response> {

    private static final String DEFAULT_USER_AGENT = 
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    private static final int DEFAULT_TIMEOUT = 30000; // 30 秒超时

    /**
     * 抓取网页内容
     * 
     * @param request 抓取请求
     * @return 抓取结果
     */
    @Tool(name = "scrapeWebPage", description = "Scrape and extract content from web pages. Supports extracting clean text, main content, or full HTML from URLs.")
    @Override
    public Response apply(@ToolParam(description = "Web scraping request with URL and extraction mode") Request request) {
        log.info("开始抓取网页: {}", request.url);
        
        Response response = new Response();
        response.url = request.url;
        
        int maxRetries = 3;
        int timeout = request.timeout > 0 ? request.timeout : DEFAULT_TIMEOUT;
        Document doc = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("尝试抓取 (第{}次): {}", attempt, request.url);
                
                // 使用 curl 命令抓取（更可靠）
                String userAgent = request.userAgent != null ? request.userAgent : DEFAULT_USER_AGENT;
                ProcessBuilder pb = new ProcessBuilder(
                    "curl", "-s", "-L", "--connect-timeout", "30",
                    "-A", userAgent,
                    "-m", "30",
                    request.url
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line).append("\n");
                }
                reader.close();
                
                int exitCode;
                try {
                    exitCode = process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("抓取被中断");
                }
                if (exitCode != 0) {
                    throw new IOException("curl 失败，退出码: " + exitCode);
                }
                
                String htmlContent = html.toString();
                if (htmlContent.isEmpty()) {
                    throw new IOException("curl 返回空内容");
                }
                
                // 解析 HTML
                doc = Jsoup.parse(htmlContent, request.url);
                log.info("curl 成功获取内容，长度: {} 字符", htmlContent.length());
                
                // 如果成功，跳出重试循环
                break;
                
            } catch (IOException e) {
                log.warn("第{}次抓取失败: {}", attempt, e.getMessage());
                
                // 如果是最后一次尝试，返回错误
                if (attempt == maxRetries) {
                    String errorMsg = e.getMessage();
                    if (errorMsg.contains("timeout") || errorMsg.contains("Timeout")) {
                        response.success = false;
                        response.error = "连接超时，可能是网络问题或目标网站无法访问。请稍后重试，或尝试其他网站。";
                    } else if (errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
                        response.success = false;
                        response.error = "目标网站拒绝访问 (403 Forbidden)，可能是反爬虫机制。请尝试其他网站。";
                    } else if (errorMsg.contains("404") || errorMsg.contains("Not Found")) {
                        response.success = false;
                        response.error = "页面不存在 (404)。";
                    } else {
                        response.success = false;
                        response.error = "抓取失败: " + errorMsg;
                    }
                    log.error("抓取最终失败: {}", e.getMessage());
                    return response;
                }
                
                // 等待后重试
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 检查是否成功获取文档
        if (doc == null) {
            response.success = false;
            response.error = "无法获取网页内容";
            return response;
        }
        
        // 提取基本信息
        response.title = doc.title();
            response.success = true;
            
            // 调试信息：记录原始 HTML 长度
            int htmlLength = doc.html().length();
            log.debug("原始 HTML 长度: {} 字符", htmlLength);
            
            // 根据提取模式处理
            switch (request.extractMode) {
                case FULL_HTML:
                    response.content = doc.html();
                    break;
                    
                case CLEAN_TEXT:
                    response.content = extractCleanText(doc);
                    break;
                    
                case MAIN_CONTENT:
                    response.content = extractMainContent(doc);
                    break;
                    
                case CUSTOM:
                    if (request.cssSelector != null) {
                        response.content = extractBySelector(doc, request.cssSelector);
                    } else {
                        response.content = extractCleanText(doc);
                    }
                    break;
            }
            
            // 如果提取的内容太短，可能是提取失败，尝试备用方案
            if (response.content.length() < 200 && request.extractMode != ExtractMode.FULL_HTML) {
                log.warn("提取内容过短（{}字符），尝试使用 CLEAN_TEXT 模式", response.content.length());
                response.content = extractCleanText(doc);
                
                // 如果还是太短，返回完整 body 文本
                if (response.content.length() < 200) {
                    log.warn("CLEAN_TEXT 仍然过短，使用完整 body 文本");
                    response.content = doc.body().text();
                }
            }
            
            // 提取元数据
            response.description = extractMetaDescription(doc);
            response.keywords = extractMetaKeywords(doc);
            
            // 提取链接（如果需要）
            if (request.extractLinks) {
                response.links = extractLinks(doc);
            }
            
            // 提取图片（如果需要）
            if (request.extractImages) {
                response.images = extractImages(doc);
            }
            
            log.info("抓取成功，内容长度: {} 字符", response.content.length());
            
        return response;
    }

    /**
     * 提取干净的文本（移除脚本、样式等）
     */
    private String extractCleanText(Document doc) {
        // 移除不需要的元素（更全面的清理）
        doc.select("script, style, nav, footer, header, aside, iframe, noscript, " +
                  ".ad, .advertisement, .ads, .adsbygoogle, " +
                  ".sidebar, .menu, .navigation, .nav, " +
                  ".comment, .comments, .related, .share, .social, " +
                  ".cookie, .popup, .modal").remove();
        
        // 获取 body 的文本
        String text = doc.body().text();
        
        // 清理多余空白
        text = text.replaceAll("\\s+", " ").trim();
        
        log.debug("提取干净文本，长度: {}", text.length());
        return text;
    }

    /**
     * 提取主要内容（尝试识别文章正文）
     */
    private String extractMainContent(Document doc) {
        // 移除不需要的元素
        doc.select("script, style, nav, footer, header, aside, .ad, .advertisement, .sidebar, .menu, .comment, .related").remove();
        
        // 尝试常见的文章容器选择器（按优先级排序）
        String[] selectors = {
            "article",
            ".article-content",
            ".post-content",
            ".entry-content",
            ".content-body",
            ".article-body",
            ".post-body",
            ".entry-body",
            "main article",
            "main .content",
            ".main-content",
            "#content",
            "#main-content",
            ".content",
            "main",
            "#main"
        };
        
        // 尝试每个选择器
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text();
                // 如果内容足够长（至少 100 个字符），认为找到了主要内容
                if (text.length() > 100) {
                    log.debug("使用选择器 '{}' 提取到 {} 字符", selector, text.length());
                    return text.replaceAll("\\s+", " ").trim();
                }
            }
        }
        
        // 如果上述选择器都没找到，尝试智能提取
        // 找到文本最多的 div 或 section
        Elements divs = doc.select("div, section");
        Element longestElement = null;
        int maxLength = 0;
        
        for (Element div : divs) {
            String text = div.ownText();  // 只获取该元素自己的文本，不包括子元素
            if (text.length() > maxLength) {
                maxLength = text.length();
                longestElement = div;
            }
        }
        
        if (longestElement != null && maxLength > 100) {
            log.debug("使用智能提取，找到最长元素，长度: {}", maxLength);
            return longestElement.text().replaceAll("\\s+", " ").trim();
        }
        
        // 最后的备选方案：返回 body 的所有文本
        String bodyText = doc.body().text().replaceAll("\\s+", " ").trim();
        log.debug("使用 body 文本，长度: {}", bodyText.length());
        return bodyText;
    }

    /**
     * 使用自定义 CSS 选择器提取内容
     */
    private String extractBySelector(Document doc, String selector) {
        Elements elements = doc.select(selector);
        if (elements.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            sb.append(element.text()).append("\n");
        }
        
        return sb.toString().trim();
    }

    /**
     * 提取 meta description
     */
    private String extractMetaDescription(Document doc) {
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            return metaDesc.attr("content");
        }
        
        metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) {
            return metaDesc.attr("content");
        }
        
        return null;
    }

    /**
     * 提取 meta keywords
     */
    private String extractMetaKeywords(Document doc) {
        Element metaKeywords = doc.selectFirst("meta[name=keywords]");
        if (metaKeywords != null) {
            return metaKeywords.attr("content");
        }
        return null;
    }

    /**
     * 提取所有链接
     */
    private List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements linkElements = doc.select("a[href]");
        
        for (Element link : linkElements) {
            String href = link.attr("abs:href"); // 获取绝对 URL
            if (!href.isEmpty() && (href.startsWith("http://") || href.startsWith("https://"))) {
                links.add(href);
            }
        }
        
        return links;
    }

    /**
     * 提取所有图片
     */
    private List<String> extractImages(Document doc) {
        List<String> images = new ArrayList<>();
        Elements imgElements = doc.select("img[src]");
        
        for (Element img : imgElements) {
            String src = img.attr("abs:src"); // 获取绝对 URL
            if (!src.isEmpty()) {
                images.add(src);
            }
        }
        
        return images;
    }

    /**
     * 抓取请求
     */
    public static class Request {
        /**
         * 目标 URL（必填）
         */
        public String url;
        
        /**
         * 提取模式
         */
        public ExtractMode extractMode = ExtractMode.CLEAN_TEXT;
        
        /**
         * 自定义 CSS 选择器（仅在 CUSTOM 模式下使用）
         */
        public String cssSelector;
        
        /**
         * User-Agent
         */
        public String userAgent;
        
        /**
         * 超时时间（毫秒）
         */
        public int timeout;
        
        /**
         * 是否提取链接
         */
        public boolean extractLinks = false;
        
        /**
         * 是否提取图片
         */
        public boolean extractImages = false;

        public Request() {}

        public Request(String url) {
            this.url = url;
        }

        public Request(String url, ExtractMode extractMode) {
            this.url = url;
            this.extractMode = extractMode;
        }
    }

    /**
     * 提取模式
     */
    public enum ExtractMode {
        /**
         * 完整 HTML
         */
        FULL_HTML,
        
        /**
         * 干净的纯文本（移除脚本、样式等）
         */
        CLEAN_TEXT,
        
        /**
         * 主要内容（尝试提取文章正文）
         */
        MAIN_CONTENT,
        
        /**
         * 自定义选择器
         */
        CUSTOM
    }

    /**
     * 抓取响应
     */
    public static class Response {
        /**
         * 原始 URL
         */
        public String url;
        
        /**
         * 是否成功
         */
        public boolean success;
        
        /**
         * 错误信息
         */
        public String error;
        
        /**
         * 页面标题
         */
        public String title;
        
        /**
         * 提取的内容
         */
        public String content;
        
        /**
         * Meta description
         */
        public String description;
        
        /**
         * Meta keywords
         */
        public String keywords;
        
        /**
         * 提取的链接
         */
        public List<String> links;
        
        /**
         * 提取的图片
         */
        public List<String> images;

        /**
         * 格式化输出
         */
        public String toFormattedString() {
            if (!success) {
                return "抓取失败: " + error;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("【网页标题】\n").append(title).append("\n\n");
            
            if (description != null) {
                sb.append("【描述】\n").append(description).append("\n\n");
            }
            
            sb.append("【内容】\n");
            // 限制内容长度，避免过长
            if (content.length() > 5000) {
                sb.append(content.substring(0, 5000)).append("...\n（内容过长，已截断）");
            } else {
                sb.append(content);
            }
            
            if (links != null && !links.isEmpty()) {
                sb.append("\n\n【链接数量】").append(links.size());
            }
            
            if (images != null && !images.isEmpty()) {
                sb.append("\n【图片数量】").append(images.size());
            }
            
            return sb.toString();
        }
    }
}
