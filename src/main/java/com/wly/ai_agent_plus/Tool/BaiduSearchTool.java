package com.wly.ai_agent_plus.Tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 搜索工具
 * 
 * 基于 SearchAPI.io 的搜索 API 实现
 * 
 * 功能：
 *   - 实时搜索结果
 *   - 支持有机搜索结果、知识图谱、AI 回答等
 *   - 支持时间范围过滤
 *   - 默认使用 Google 搜索，也支持指定 Baidu 等 SearchAPI 引擎
 */
@Component
@Slf4j
public class BaiduSearchTool implements Function<BaiduSearchTool.Request, BaiduSearchTool.Response> {

    @Value("${search-api.api-key:${searchapi.api-key:}}")
    private String apiKey;

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BaiduSearchTool() {
        this.objectMapper = new ObjectMapper();
        // 配置超时
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 30秒连接超时
        factory.setReadTimeout(30000);     // 30秒读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 执行搜索
     * 
     * @param request 搜索请求
     * @return 搜索结果
     */
    @Tool(name = "searchWeb", description = "Search the web through SearchAPI. Defaults to Google search and can use other engines such as Baidu when requested. Returns organic results, answer box, knowledge graph, and related searches.")
    @Override
    public Response apply(@ToolParam(description = "Web search request with query, optional engine, and optional filters") Request request) {
        log.info("搜索: {}", request.query);
        
        try {
            if (request == null || !StringUtils.hasText(request.query)) {
                Response errorResponse = new Response();
                errorResponse.error = "搜索失败: query 不能为空";
                return errorResponse;
            }
            if (!StringUtils.hasText(apiKey)) {
                Response errorResponse = new Response();
                errorResponse.error = "搜索失败: 未配置 search-api.api-key";
                return errorResponse;
            }

            String engine = StringUtils.hasText(request.engine) ? request.engine : "google";

            // 构建请求 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SEARCH_API_URL)
                    .queryParam("engine", engine)
                    .queryParam("q", request.query)
                    .queryParam("api_key", apiKey);
            
            // 可选参数
            if (request.ct != null && request.ct >= 0 && request.ct <= 2) {
                builder.queryParam("ct", request.ct);
            }
            if (StringUtils.hasText(request.gpc)) {
                builder.queryParam("gpc", request.gpc);
            }
            
            String url = builder.toUriString();
            log.debug("请求 URL: {}", url);
            
            // 发送请求
            String jsonResponse = restTemplate.getForObject(url, String.class);
            
            // 解析响应
            JsonNode root = objectMapper.readTree(jsonResponse);
            Response response = parseResponse(root);
            
            log.info("搜索完成，找到 {} 个有机结果", response.organicResults.size());
            return response;
            
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            Response errorResponse = new Response();
            errorResponse.error = "搜索失败: " + e.getMessage();
            return errorResponse;
        }
    }

    /**
     * 解析搜索响应
     */
    private Response parseResponse(JsonNode root) {
        Response response = new Response();
        
        // 解析有机搜索结果
        if (root.has("organic_results")) {
            JsonNode organicResults = root.get("organic_results");
            for (JsonNode result : organicResults) {
                OrganicResult or = new OrganicResult();
                or.title = result.has("title") ? result.get("title").asText() : null;
                or.link = result.has("link") ? result.get("link").asText() : null;
                or.snippet = result.has("snippet") ? result.get("snippet").asText() : null;
                or.displayedLink = result.has("displayed_link") ? result.get("displayed_link").asText() : null;
                response.organicResults.add(or);
            }
        }
        
        // 解析 AI 回答框
        if (root.has("answer_box")) {
            JsonNode answerBox = root.get("answer_box");
            AnswerBox ab = new AnswerBox();
            ab.type = answerBox.has("type") ? answerBox.get("type").asText() : null;
            ab.answer = answerBox.has("answer") ? answerBox.get("answer").asText() : null;
            ab.snippet = answerBox.has("snippet") ? answerBox.get("snippet").asText() : null;
            response.answerBox = ab;
        }
        
        // 解析知识图谱
        if (root.has("knowledge_graph")) {
            JsonNode kg = root.get("knowledge_graph");
            KnowledgeGraph knowledgeGraph = new KnowledgeGraph();
            knowledgeGraph.title = kg.has("title") ? kg.get("title").asText() : null;
            knowledgeGraph.description = kg.has("description") ? kg.get("description").asText() : null;
            knowledgeGraph.source = kg.has("source") ? kg.get("source").asText() : null;
            response.knowledgeGraph = knowledgeGraph;
        }
        
        // 解析相关搜索
        if (root.has("related_searches")) {
            JsonNode relatedSearches = root.get("related_searches");
            for (JsonNode rs : relatedSearches) {
                if (rs.has("query")) {
                    response.relatedSearches.add(rs.get("query").asText());
                }
            }
        }
        
        return response;
    }

    /**
     * 搜索请求
     */
    public static class Request {
        /**
         * 搜索关键词（必填）
         */
        public String query;

        /**
         * 搜索引擎（可选）
         * 默认 google；也可以传 baidu 等 SearchAPI 支持的引擎。
         */
        public String engine;
        
        /**
         * 语言控制（可选）
         * 0 - 简体和繁体中文（默认）
         * 1 - 简体中文
         * 2 - 繁体中文
         */
        public Integer ct;
        
        /**
         * 时间范围过滤（可选）
         * 格式: stf=START_TIME,END_TIME|stftype=1
         * 其中 START_TIME 和 END_TIME 是 Unix 时间戳
         */
        public String gpc;

        public Request() {}

        public Request(String query) {
            this.query = query;
        }

        public Request(String query, Integer ct) {
            this.query = query;
            this.ct = ct;
        }

        public Request(String query, String engine) {
            this.query = query;
            this.engine = engine;
        }
    }

    /**
     * 搜索响应
     */
    public static class Response {
        /**
         * 有机搜索结果
         */
        public List<OrganicResult> organicResults = new ArrayList<>();
        
        /**
         * AI 回答框
         */
        public AnswerBox answerBox;
        
        /**
         * 知识图谱
         */
        public KnowledgeGraph knowledgeGraph;
        
        /**
         * 相关搜索
         */
        public List<String> relatedSearches = new ArrayList<>();
        
        /**
         * 错误信息
         */
        public String error;

        /**
         * 格式化输出搜索结果
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            
            if (error != null) {
                sb.append("错误: ").append(error).append("\n");
                return sb.toString();
            }
            
            // AI 回答
            if (answerBox != null && answerBox.answer != null) {
                sb.append("【AI 回答】\n");
                sb.append(answerBox.answer).append("\n\n");
            }
            
            // 知识图谱
            if (knowledgeGraph != null) {
                sb.append("【知识图谱】\n");
                sb.append("标题: ").append(knowledgeGraph.title).append("\n");
                if (knowledgeGraph.description != null) {
                    sb.append("描述: ").append(knowledgeGraph.description).append("\n");
                }
                sb.append("\n");
            }
            
            // 有机搜索结果
            if (!organicResults.isEmpty()) {
                sb.append("【搜索结果】\n");
                for (int i = 0; i < Math.min(5, organicResults.size()); i++) {
                    OrganicResult result = organicResults.get(i);
                    sb.append(i + 1).append(". ").append(result.title).append("\n");
                    if (result.snippet != null) {
                        sb.append("   ").append(result.snippet).append("\n");
                    }
                    sb.append("   链接: ").append(result.link).append("\n\n");
                }
            }
            
            // 相关搜索
            if (!relatedSearches.isEmpty()) {
                sb.append("【相关搜索】\n");
                for (String rs : relatedSearches) {
                    sb.append("- ").append(rs).append("\n");
                }
            }
            
            return sb.toString();
        }
    }

    /**
     * 有机搜索结果
     */
    public static class OrganicResult {
        public String title;
        public String link;
        public String snippet;
        public String displayedLink;
    }

    /**
     * AI 回答框
     */
    public static class AnswerBox {
        public String type;
        public String answer;
        public String snippet;
    }

    /**
     * 知识图谱
     */
    public static class KnowledgeGraph {
        public String title;
        public String description;
        public String source;
    }
}
