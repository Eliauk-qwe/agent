# 网页抓取工具使用指南

## 概述

基于 Jsoup 实现的网页抓取工具，可以抓取和解析网页内容。

## 功能特性

- ✅ 抓取网页完整内容
- ✅ 多种提取模式（完整 HTML、干净文本、主要内容）
- ✅ 自定义 CSS 选择器
- ✅ 提取链接和图片
- ✅ 提取 Meta 信息
- ✅ 自动清理无用内容（脚本、样式、广告等）
- ✅ 支持自定义 User-Agent
- ✅ 超时控制

## 快速开始

### 1. 基础抓取

```java
@Autowired
private WebScraperTool webScraperTool;

public void basicScraping() {
    // 创建请求
    WebScraperTool.Request request = new WebScraperTool.Request("https://example.com");
    
    // 执行抓取
    WebScraperTool.Response response = webScraperTool.apply(request);
    
    // 使用结果
    if (response.success) {
        System.out.println("标题: " + response.title);
        System.out.println("内容: " + response.content);
    }
}
```

### 2. 提取模式

#### 模式 1: 干净文本（默认）

```java
WebScraperTool.Request request = new WebScraperTool.Request(
    "https://example.com",
    WebScraperTool.ExtractMode.CLEAN_TEXT
);
```

移除脚本、样式、导航等，只保留文本内容。

#### 模式 2: 主要内容

```java
WebScraperTool.Request request = new WebScraperTool.Request(
    "https://example.com",
    WebScraperTool.ExtractMode.MAIN_CONTENT
);
```

尝试识别并提取文章正文，适合新闻、博客等。

#### 模式 3: 完整 HTML

```java
WebScraperTool.Request request = new WebScraperTool.Request(
    "https://example.com",
    WebScraperTool.ExtractMode.FULL_HTML
);
```

返回完整的 HTML 代码。

#### 模式 4: 自定义选择器

```java
WebScraperTool.Request request = new WebScraperTool.Request("https://example.com");
request.extractMode = WebScraperTool.ExtractMode.CUSTOM;
request.cssSelector = "article p";  // 只提取 article 中的段落
```

使用 CSS 选择器精确提取内容。

### 3. 提取链接和图片

```java
WebScraperTool.Request request = new WebScraperTool.Request("https://example.com");
request.extractLinks = true;   // 提取所有链接
request.extractImages = true;  // 提取所有图片

WebScraperTool.Response response = webScraperTool.apply(request);

System.out.println("链接数量: " + response.links.size());
System.out.println("图片数量: " + response.images.size());
```

### 4. 自定义配置

```java
WebScraperTool.Request request = new WebScraperTool.Request("https://example.com");
request.userAgent = "MyBot/1.0";  // 自定义 User-Agent
request.timeout = 15000;          // 15 秒超时
```

## 组合使用：搜索 + 抓取

### 完整流程示例

```java
@Autowired
private SearchAndScrapDemo demo;

public void example() {
    // 搜索 → 抓取 → AI 总结
    String answer = demo.searchScrapAndAnswer("什么是 Spring AI");
    System.out.println(answer);
}
```

### 工作流程

```
1. 百度搜索 "什么是 Spring AI"
   ↓
2. 获取搜索结果列表
   ↓
3. 抓取排名第一的网页完整内容
   ↓
4. AI 基于完整内容生成回答
```

## 使用场景

### 场景 1: 快速了解（只搜索）

```java
// 适合：快速了解某个话题
BaiduSearchTool.Response response = baiduSearchTool.apply(
    new BaiduSearchTool.Request("Spring AI")
);
// 得到：多个网页的标题和摘要
```

### 场景 2: 深入研究（搜索 + 抓取）

```java
// 适合：需要详细信息
String answer = demo.searchScrapAndAnswer("Spring AI 详细教程");
// 得到：基于完整文章的详细回答
```

### 场景 3: 综合分析（搜索 + 多页抓取）

```java
// 适合：需要多个来源的综合信息
String analysis = demo.deepResearch("人工智能趋势", 3);
// 得到：综合 3 个网页的分析报告
```

### 场景 4: 特定信息提取

```java
// 适合：只需要网页的特定部分
String info = demo.extractSpecificInfo(
    "https://example.com",
    ".price",  // 只提取价格信息
    "这个产品多少钱？"
);
```

## CSS 选择器示例

### 常用选择器

```css
/* 标签选择器 */
p                    /* 所有段落 */
h1, h2, h3          /* 所有标题 */
a                    /* 所有链接 */

/* 类选择器 */
.article-content     /* class="article-content" */
.post .content       /* post 下的 content */

/* ID 选择器 */
#main-content        /* id="main-content" */

/* 属性选择器 */
a[href]              /* 有 href 属性的链接 */
img[src]             /* 有 src 属性的图片 */

/* 组合选择器 */
article p            /* article 中的段落 */
div.content > p      /* content 的直接子段落 */
```

### 实际示例

```java
// 提取新闻标题
request.cssSelector = "h1.news-title";

// 提取文章段落
request.cssSelector = "article p";

// 提取价格信息
request.cssSelector = ".price, .product-price";

// 提取评论
request.cssSelector = ".comment-content";
```

## 注意事项

### 1. 法律和道德

- ⚠️ 遵守网站的 robots.txt
- ⚠️ 不要频繁请求，避免给服务器造成压力
- ⚠️ 尊重网站的服务条款
- ⚠️ 不要用于商业目的（除非获得许可）

### 2. 技术限制

- 🔒 某些网站有反爬虫机制
- 🔒 动态加载的内容（JavaScript）可能抓取不到
- 🔒 需要登录的内容无法抓取
- 🔒 可能被 IP 封禁

### 3. 性能优化

```java
// 添加延迟，避免请求过快
Thread.sleep(1000);  // 每次请求间隔 1 秒

// 使用缓存，避免重复抓取
@Cacheable(value = "webContent", key = "#url")
public Response scrape(String url) {
    // ...
}

// 异步抓取
@Async
public CompletableFuture<Response> scrapeAsync(String url) {
    // ...
}
```

### 4. 错误处理

```java
WebScraperTool.Response response = webScraperTool.apply(request);

if (!response.success) {
    // 处理错误
    System.err.println("抓取失败: " + response.error);
    
    // 可能的错误：
    // - 网络超时
    // - 404 Not Found
    // - 403 Forbidden
    // - 连接被拒绝
}
```

## 高级用法

### 1. 批量抓取

```java
List<String> urls = Arrays.asList(
    "https://example1.com",
    "https://example2.com",
    "https://example3.com"
);

List<WebScraperTool.Response> responses = urls.stream()
    .map(url -> new WebScraperTool.Request(url))
    .map(webScraperTool::apply)
    .filter(r -> r.success)
    .collect(Collectors.toList());
```

### 2. 递归抓取（爬取整个网站）

```java
public void crawlWebsite(String startUrl, int maxDepth) {
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new LinkedList<>();
    queue.add(startUrl);
    
    while (!queue.isEmpty() && visited.size() < 100) {
        String url = queue.poll();
        if (visited.contains(url)) continue;
        
        // 抓取页面
        WebScraperTool.Request request = new WebScraperTool.Request(url);
        request.extractLinks = true;
        WebScraperTool.Response response = webScraperTool.apply(request);
        
        if (response.success) {
            visited.add(url);
            // 处理内容...
            
            // 添加新链接到队列
            if (visited.size() < maxDepth) {
                response.links.stream()
                    .filter(link -> link.startsWith(startUrl))
                    .forEach(queue::add);
            }
        }
        
        // 延迟，避免请求过快
        Thread.sleep(1000);
    }
}
```

### 3. 定时抓取（监控网站更新）

```java
@Scheduled(fixedRate = 3600000)  // 每小时执行一次
public void monitorWebsite() {
    WebScraperTool.Response response = webScraperTool.apply(
        new WebScraperTool.Request("https://example.com/news")
    );
    
    if (response.success) {
        // 检查内容是否有更新
        if (hasNewContent(response.content)) {
            // 发送通知
            notifyUser("网站有新内容更新！");
        }
    }
}
```

## 测试

运行测试查看效果：

```bash
# 测试基础抓取功能
mvn test -Dtest=WebScraperToolTest

# 测试搜索 + 抓取组合
mvn test -Dtest=SearchAndScrapDemoTest
```

## 常见问题

**Q: 为什么抓取的内容是空的？**
A: 可能是动态加载的内容（JavaScript），Jsoup 无法执行 JS。可以尝试使用 Selenium。

**Q: 如何处理需要登录的网站？**
A: Jsoup 支持 Cookie，可以先登录获取 Cookie，然后在请求中携带。

**Q: 如何提高抓取速度？**
A: 使用异步抓取、并发请求、缓存结果。

**Q: 如何避免被封禁？**
A: 添加延迟、使用代理、轮换 User-Agent、遵守 robots.txt。

## 相关资源

- [Jsoup 官方文档](https://jsoup.org/)
- [CSS 选择器参考](https://www.w3schools.com/cssref/css_selectors.asp)
- [robots.txt 规范](https://www.robotstxt.org/)

## 更新日志

### v1.0.0 (2026-04-25)
- ✅ 初始版本
- ✅ 支持多种提取模式
- ✅ 支持自定义 CSS 选择器
- ✅ 支持提取链接和图片
- ✅ 集成搜索 + 抓取组合功能



