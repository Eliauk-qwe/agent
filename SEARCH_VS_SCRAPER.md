# 联网搜索 vs 网页抓取 - 完整对比

## 快速对比表

| 特性 | 联网搜索 (BaiduSearchTool) | 网页抓取 (WebScraperTool) |
|------|---------------------------|--------------------------|
| **主要功能** | 搜索引擎查询 | 抓取网页内容 |
| **返回内容** | 搜索结果摘要 | 完整网页内容 |
| **速度** | 快（1-2秒） | 中等（3-5秒/页） |
| **信息广度** | 高（多个来源） | 低（单个页面） |
| **信息深度** | 低（只有摘要） | 高（完整内容） |
| **成本** | 付费 API | 免费 |
| **法律风险** | 低（官方 API） | 中（可能违反 ToS） |
| **反爬风险** | 无 | 有 |
| **适用场景** | 快速了解、多源信息 | 深入研究、完整内容 |

## 工具介绍

### 1. BaiduSearchTool - 联网搜索

**位置**: `src/main/java/com/wly/ai_agent_plus/Tool/BaiduSearchTool.java`

**功能**:
- 调用百度搜索 API
- 获取搜索结果列表
- 包含标题、摘要、链接
- 可能包含 AI 回答、知识图谱

**示例**:
```java
BaiduSearchTool.Request request = new BaiduSearchTool.Request("Spring AI");
BaiduSearchTool.Response response = baiduSearchTool.apply(request);

// 结果：
// 1. Spring AI 官方文档 - Spring AI 是一个用于构建...
// 2. Spring AI 教程 - 本教程介绍如何使用...
// 3. Spring AI GitHub - 开源项目地址...
```

### 2. WebScraperTool - 网页抓取

**位置**: `src/main/java/com/wly/ai_agent_plus/Tool/WebScraperTool.java`

**功能**:
- 抓取指定 URL 的网页
- 解析 HTML 内容
- 提取文本、链接、图片
- 支持 CSS 选择器

**示例**:
```java
WebScraperTool.Request request = new WebScraperTool.Request(
    "https://docs.spring.io/spring-ai"
);
WebScraperTool.Response response = webScraperTool.apply(request);

// 结果：完整的文档内容（几千字）
```

## 使用场景对比

### 场景 1: 快速了解某个话题

**需求**: 我想知道 "Spring AI 是什么"

**推荐**: 联网搜索 ✅

**原因**:
- 快速获取多个来源的摘要
- 可能直接有 AI 回答
- 不需要完整内容

```java
BaiduSearchTool.Response response = baiduSearchTool.apply(
    new BaiduSearchTool.Request("Spring AI 是什么")
);

// 可能直接得到 AI 回答：
// "Spring AI 是一个用于构建 AI 应用的 Java 框架..."
```

---

### 场景 2: 深入学习某个技术

**需求**: 我想详细学习 Spring AI 的使用方法

**推荐**: 搜索 + 抓取 ✅

**原因**:
- 需要完整的教程内容
- 需要代码示例
- 需要详细说明

```java
// 1. 先搜索找到最好的教程
BaiduSearchTool.Response searchResponse = baiduSearchTool.apply(
    new BaiduSearchTool.Request("Spring AI 详细教程")
);

// 2. 抓取排名第一的教程
String url = searchResponse.organicResults.get(0).link;
WebScraperTool.Response scrapResponse = webScraperTool.apply(
    new WebScraperTool.Request(url)
);

// 3. 得到完整教程内容
```

---

### 场景 3: 对比分析

**需求**: 对比 Spring Boot 和 Spring Cloud

**方案 A**: 只用搜索（快速对比）

```java
// 搜索两个话题，基于摘要对比
BaiduSearchTool.Response response1 = baiduSearchTool.apply(
    new BaiduSearchTool.Request("Spring Boot")
);
BaiduSearchTool.Response response2 = baiduSearchTool.apply(
    new BaiduSearchTool.Request("Spring Cloud")
);

// AI 基于搜索摘要对比
```

**方案 B**: 搜索 + 抓取（深度对比）

```java
// 搜索并抓取完整内容，深度对比
String answer = demo.compareWithFullContent("Spring Boot", "Spring Cloud");
```

---

### 场景 4: 监控网站更新

**需求**: 监控某个新闻网站是否有新文章

**推荐**: 网页抓取 ✅

**原因**:
- 需要定期检查特定网页
- 不需要搜索功能
- 直接抓取更高效

```java
@Scheduled(fixedRate = 3600000)  // 每小时
public void checkNews() {
    WebScraperTool.Response response = webScraperTool.apply(
        new WebScraperTool.Request("https://news.example.com")
    );
    
    if (hasNewArticles(response.content)) {
        notifyUser();
    }
}
```

---

### 场景 5: 提取特定信息

**需求**: 从产品页面提取价格信息

**推荐**: 网页抓取 + CSS 选择器 ✅

**原因**:
- 需要精确提取特定元素
- 搜索无法提供这种精度

```java
WebScraperTool.Request request = new WebScraperTool.Request(
    "https://shop.example.com/product/123"
);
request.extractMode = WebScraperTool.ExtractMode.CUSTOM;
request.cssSelector = ".price";

WebScraperTool.Response response = webScraperTool.apply(request);
// 得到：¥199.00
```

## 组合使用策略

### 策略 1: 搜索 → 抓取 → 总结

**最常用的模式**

```java
// 使用 SearchAndScrapDemo
String answer = demo.searchScrapAndAnswer("如何学习 Spring AI");

// 流程：
// 1. 百度搜索 "如何学习 Spring AI"
// 2. 抓取排名第一的网页
// 3. AI 基于完整内容生成回答
```

**适用场景**:
- 需要权威、详细的答案
- 用户问题比较复杂
- 摘要信息不够用

---

### 策略 2: 搜索 → 多页抓取 → 综合分析

**深度研究模式**

```java
String analysis = demo.deepResearch("人工智能趋势", 3);

// 流程：
// 1. 百度搜索 "人工智能趋势"
// 2. 抓取前 3 个网页的完整内容
// 3. AI 综合分析多个来源
```

**适用场景**:
- 需要多个观点
- 需要全面的信息
- 研究性问题

---

### 策略 3: 只搜索（快速模式）

**最快速的模式**

```java
String answer = simpleBaiduSearchDemo.searchAndAnswer("什么是 AI");

// 流程：
// 1. 百度搜索
// 2. AI 基于搜索摘要回答
// （不抓取网页）
```

**适用场景**:
- 简单问题
- 需要快速响应
- 摘要信息足够

---

### 策略 4: 只抓取（已知 URL）

**直接抓取模式**

```java
WebScraperTool.Response response = webScraperTool.apply(
    new WebScraperTool.Request("https://docs.spring.io/spring-ai")
);
```

**适用场景**:
- 已知目标 URL
- 不需要搜索
- 定期监控

## 性能对比

### 响应时间

```
只搜索:           1-2 秒
搜索 + 抓取 1 页:  4-7 秒
搜索 + 抓取 3 页:  10-20 秒
只抓取:           3-5 秒
```

### 信息质量

```
只搜索:           ⭐⭐⭐ (摘要信息)
搜索 + 抓取 1 页:  ⭐⭐⭐⭐ (详细信息)
搜索 + 抓取 3 页:  ⭐⭐⭐⭐⭐ (全面信息)
只抓取:           ⭐⭐⭐⭐ (单源详细)
```

## 成本对比

### 联网搜索 (BaiduSearchTool)

- **API 费用**: 需要购买 SearchAPI.io 的服务
- **免费额度**: 通常有限（如 100 次/月）
- **付费价格**: 约 $0.01-0.05 / 次

### 网页抓取 (WebScraperTool)

- **API 费用**: 无
- **服务器成本**: 带宽和计算资源
- **风险成本**: 可能被封禁

## 法律和道德考虑

### 联网搜索

✅ **合法性**: 使用官方 API，完全合法
✅ **道德性**: 符合搜索引擎的使用条款
✅ **风险**: 低

### 网页抓取

⚠️ **合法性**: 取决于网站的服务条款
⚠️ **道德性**: 需要遵守 robots.txt
⚠️ **风险**: 中等

**建议**:
1. 检查网站的 robots.txt
2. 添加合理的延迟
3. 不要用于商业目的（除非获得许可）
4. 尊重网站的反爬虫措施

## 实际项目中的选择

### 恋爱助手项目

**场景 1**: 用户问 "最近流行的约会方式"

```java
// 推荐：只搜索（快速）
String answer = simpleBaiduSearchDemo.searchAndAnswer("2026年流行的约会方式");
```

**场景 2**: 用户问 "如何经营长期关系"

```java
// 推荐：搜索 + 抓取（详细）
String answer = demo.searchScrapAndAnswer("如何经营长期恋爱关系");
```

**场景 3**: 用户问 "这篇文章怎么样" + 提供链接

```java
// 推荐：只抓取（已知 URL）
WebScraperTool.Response response = webScraperTool.apply(
    new WebScraperTool.Request(userProvidedUrl)
);
```

## 总结

### 何时使用联网搜索

- ✅ 需要快速响应
- ✅ 需要多个信息源
- ✅ 问题比较简单
- ✅ 摘要信息足够
- ✅ 有 API 预算

### 何时使用网页抓取

- ✅ 需要完整内容
- ✅ 已知目标 URL
- ✅ 需要特定信息
- ✅ 定期监控
- ✅ 没有 API 预算

### 何时组合使用

- ✅ 需要权威详细的答案
- ✅ 研究性问题
- ✅ 需要多源综合分析
- ✅ 用户体验要求高

## 代码示例总结

```java
// 1. 只搜索（最快）
simpleBaiduSearchDemo.searchAndAnswer(question);

// 2. 搜索 + 抓取（最常用）
searchAndScrapDemo.searchScrapAndAnswer(question);

// 3. 深度研究（最全面）
searchAndScrapDemo.deepResearch(topic, 3);

// 4. 只抓取（已知 URL）
webScraperTool.apply(new WebScraperTool.Request(url));

// 5. 精确提取（特定信息）
searchAndScrapDemo.extractSpecificInfo(url, cssSelector, question);
```

选择合适的工具和策略，可以在性能、成本和信息质量之间找到最佳平衡！
