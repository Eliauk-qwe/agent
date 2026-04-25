# 百度搜索工具使用指南

## 概述

基于 [SearchAPI.io](https://www.searchapi.io/docs/baidu) 实现的百度搜索工具，可以让 AI Agent 实时获取百度搜索结果。

## 功能特性

- ✅ 实时百度搜索结果
- ✅ 有机搜索结果（Organic Results）
- ✅ AI 回答框（Answer Box）
- ✅ 知识图谱（Knowledge Graph）
- ✅ 相关搜索建议
- ✅ 支持简体/繁体中文
- ✅ 支持时间范围过滤
- ✅ 集成到 Spring AI Function Calling

## 快速开始

### 1. 获取 API Key

1. 访问 [SearchAPI.io](https://www.searchapi.io/)
2. 注册账号
3. 获取 API Key

### 2. 配置 API Key

在 `src/main/resources/application.yml` 中配置：

```yaml
searchapi:
  api-key: YOUR_SEARCHAPI_KEY  # 替换为你的 API Key
```

### 3. 使用方式

#### ⭐ 推荐方式: 简单直接（无需 Function Calling 配置）

```java
@Autowired
private SimpleBaiduSearchDemo demo;

public void example() {
    // 搜索 + AI 回答
    String answer = demo.searchAndAnswer("什么是 Spring AI？");
    System.out.println(answer);
    
    // 恋爱建议 + 实时搜索
    String advice = demo.getLoveAdviceWithSearch("异地恋如何维持");
    System.out.println(advice);
    
    // 获取最新趋势
    String trends = demo.getLatestTrends("人工智能");
    System.out.println(trends);
}
```

#### 方式 1: 直接调用搜索工具

```java
@Autowired
private BaiduSearchTool baiduSearchTool;

public void search() {
    // 创建搜索请求
    BaiduSearchTool.Request request = new BaiduSearchTool.Request("Spring AI");
    
    // 执行搜索
    BaiduSearchTool.Response response = baiduSearchTool.apply(request);
    
    // 输出结果
    System.out.println(response.toFormattedString());
}
```

#### 方式 2: 搜索 + AI 总结（推荐）

```java
@Autowired
private BaiduSearchTool baiduSearchTool;

@Autowired
private ChatClient.Builder chatClientBuilder;

public String searchAndSummarize(String topic) {
    // 1. 搜索
    BaiduSearchTool.Request request = new BaiduSearchTool.Request(topic);
    BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(request);
    
    // 2. AI 总结
    ChatClient chatClient = chatClientBuilder.build();
    String prompt = String.format(
        "请总结以下搜索结果：\n%s", 
        searchResponse.toFormattedString()
    );
    
    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
}
```

#### 方式 3: 集成到 ChatClient（Function Calling）

```java
@Autowired
private ChatClient.Builder chatClientBuilder;

public void chatWithSearch() {
    ChatClient chatClient = chatClientBuilder
        .defaultSystem("你是一个智能助手，可以使用百度搜索获取最新信息。")
        .build();
    
    String response = chatClient.prompt()
        .user("最新的 AI 技术发展趋势是什么？")
        .functions("baiduSearch")  // 在 prompt 级别注册搜索工具
        .call()
        .content();
    
    System.out.println(response);
}
```

#### 方式 3: 搜索后让 AI 总结

```java
public String searchAndSummarize(String topic) {
    // 1. 搜索
    BaiduSearchTool.Request request = new BaiduSearchTool.Request(topic);
    BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(request);
    
    // 2. AI 总结
    ChatClient chatClient = chatClientBuilder.build();
    String prompt = String.format(
        "请总结以下搜索结果：\n%s", 
        searchResponse.toFormattedString()
    );
    
    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
}
```

## API 参数说明

### Request 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 搜索关键词 |
| ct | Integer | 否 | 语言控制：0=简繁体（默认），1=简体，2=繁体 |
| gpc | String | 否 | 时间范围过滤，格式：`stf=START_TIME,END_TIME\|stftype=1` |

### Response 结构

```java
Response {
    List<OrganicResult> organicResults;  // 有机搜索结果
    AnswerBox answerBox;                 // AI 回答框
    KnowledgeGraph knowledgeGraph;       // 知识图谱
    List<String> relatedSearches;        // 相关搜索
    String error;                        // 错误信息
}
```

## 使用示例

### 示例 1: 基础搜索

```java
BaiduSearchTool.Request request = new BaiduSearchTool.Request("ChatGPT");
BaiduSearchTool.Response response = baiduSearchTool.apply(request);
System.out.println(response.toFormattedString());
```

### 示例 2: 简体中文搜索

```java
BaiduSearchTool.Request request = new BaiduSearchTool.Request("人工智能", 1);
BaiduSearchTool.Response response = baiduSearchTool.apply(request);
```

### 示例 3: 时间范围搜索

```java
// 搜索最近一年的结果
long now = System.currentTimeMillis() / 1000;
long oneYearAgo = now - (365 * 24 * 60 * 60);
String gpc = String.format("stf=%d,%d|stftype=1", oneYearAgo, now);

BaiduSearchTool.Request request = new BaiduSearchTool.Request("AI 新闻");
request.gpc = gpc;
BaiduSearchTool.Response response = baiduSearchTool.apply(request);
```

### 示例 4: 在恋爱助手中使用

```java
public void chatWithRealTimeInfo(String message, String chatId) {
    ChatClient chatClient = chatClientBuilder
        .defaultSystem(systemPrompt + """
            
            当用户询问最新的恋爱趋势、热门话题或需要实时信息时，
            你可以使用 baiduSearch 工具搜索相关内容。
            """)
        .build();
    
    String response = chatClient.prompt()
        .user(message)
        .functions("baiduSearch")  // 注册搜索工具
        .advisors(spec -> spec
            .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build())
        )
        .call()
        .content();
    
    System.out.println(response);
}
```

## 测试

运行测试类验证功能：

```bash
mvn test -Dtest=BaiduSearchToolTest
```

测试用例包括：
- ✅ 基础搜索
- ✅ 中文搜索
- ✅ AI 回答测试
- ✅ 知识图谱测试

## 注意事项

1. **API Key 安全**
   - 不要将 API Key 提交到代码仓库
   - 使用环境变量或配置文件管理
   - 生产环境使用密钥管理服务

2. **API 限制**
   - 免费版有请求次数限制
   - 注意控制调用频率
   - 建议添加缓存机制

3. **错误处理**
   - 工具已内置错误处理
   - 检查 `response.error` 字段
   - 网络异常会返回错误信息

4. **性能优化**
   - 搜索结果可以缓存
   - 避免重复搜索相同关键词
   - 考虑异步调用

## 高级用法

### 1. 添加缓存

```java
@Cacheable(value = "baiduSearch", key = "#request.query")
public Response apply(Request request) {
    // ... 搜索逻辑
}
```

### 2. 异步搜索

```java
@Async
public CompletableFuture<Response> searchAsync(Request request) {
    return CompletableFuture.completedFuture(apply(request));
}
```

### 3. 批量搜索

```java
public List<Response> batchSearch(List<String> queries) {
    return queries.stream()
        .map(q -> new Request(q))
        .map(this::apply)
        .collect(Collectors.toList());
}
```

## 相关资源

- [SearchAPI.io 官方文档](https://www.searchapi.io/docs/baidu)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Function Calling 指南](https://docs.spring.io/spring-ai/reference/api/functions.html)

## 常见问题

**Q: 如何获取更多搜索结果？**
A: 默认返回前 10 条，可以通过 API 参数调整（需查看 SearchAPI 文档）。

**Q: 支持图片搜索吗？**
A: 当前版本主要支持文本搜索，图片搜索需要额外实现。

**Q: 如何处理搜索失败？**
A: 检查 `response.error` 字段，包含详细错误信息。

**Q: 可以搜索英文内容吗？**
A: 可以，百度支持多语言搜索。

## 更新日志

### v1.0.0 (2026-04-25)
- ✅ 初始版本
- ✅ 支持基础搜索功能
- ✅ 集成 Spring AI Function Calling
- ✅ 支持 AI 回答、知识图谱等特性
