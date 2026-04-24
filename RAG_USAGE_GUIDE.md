# RAG 文档加载器使用指南

## 快速开始

### 1. 基本使用

文档加载器会自动加载 `src/main/resources/document/` 目录下的所有 Markdown 文件。

```java
@Autowired
private LoveAppDocumentLoader documentLoader;

public void loadDocuments() {
    List<Document> documents = documentLoader.loadmarkdowns();
    System.out.println("加载了 " + documents.size() + " 个文档片段");
}
```

### 2. 配置文档位置

在 `application.yml` 中配置文档位置：

```yaml
spring:
  ai:
    document:
      location: classpath:document/*.md  # 默认位置
      # 或者使用文件系统路径
      # location: file:/path/to/documents/*.md
```

### 3. 调整切片参数

根据你的需求调整切片参数：

```yaml
spring:
  ai:
    document:
      chunk-size: 800              # 每个切片的目标 token 数
      min-chunk-size-chars: 350    # 最小字符数
      chunk-overlap: 100           # 切片重叠字符数
```

#### 参数说明

- **chunk-size**: 控制每个切片的大小（以 token 为单位）
  - 较小的值（400-600）：更精确的检索，但可能丢失上下文
  - 较大的值（1000-1500）：更多上下文，但检索精度可能降低
  - 推荐值：800

- **min-chunk-size-chars**: 最小字符数
  - 防止生成过小的切片
  - 推荐值：350

- **chunk-overlap**: 切片重叠
  - 在切片边界保留上下文
  - 推荐值：chunk-size 的 10-15%

### 4. 启用缓存

缓存可以显著提升应用启动速度：

```yaml
spring:
  cache:
    type: simple  # 开发环境
    # type: redis  # 生产环境
    cache-names: markdownDocuments
```

确保主应用类添加了 `@EnableCaching` 注解：

```java
@SpringBootApplication
@EnableCaching
public class AiAgentPlusApplication {
    // ...
}
```

### 5. 查看文档元数据

每个文档片段都包含丰富的元数据：

```java
Document doc = documents.get(0);
Map<String, Object> metadata = doc.getMetadata();

String filename = (String) metadata.get("filename");
String heading = (String) metadata.get("heading");
Integer level = (Integer) metadata.get("level");
Integer chunkIndex = (Integer) metadata.get("chunk_index");
String headingPath = (String) metadata.get("heading_path");
Integer contentLength = (Integer) metadata.get("content_length");
String documentType = (String) metadata.get("document_type");
Long loadedAt = (Long) metadata.get("loaded_at");
```

## 高级用法

### 1. 自定义文档格式

如果需要支持其他文档格式，可以扩展 `loadSingleDocument` 方法：

```java
private List<Document> loadSingleDocument(Resource resource) throws IOException {
    String filename = resource.getFilename();
    if (filename == null) {
        filename = "unknown";
    }
    
    // 根据文件扩展名选择不同的 Reader
    DocumentReader reader;
    if (filename.endsWith(".pdf")) {
        reader = new PdfDocumentReader(resource);
    } else if (filename.endsWith(".docx")) {
        reader = new WordDocumentReader(resource);
    } else {
        reader = new TextReader(resource);
    }
    
    List<Document> rawDocuments = reader.get();
    // ... 后续处理
}
```

### 2. 自定义切片策略

如果需要不同的切片策略，可以创建自定义的 `TextSplitter`：

```java
// 基于段落的切片
TextSplitter paragraphSplitter = new ParagraphTextSplitter(
    maxParagraphLength,
    minParagraphLength
);

// 基于句子的切片
TextSplitter sentenceSplitter = new SentenceTextSplitter(
    maxSentences,
    minSentences
);
```

### 3. 向量存储持久化

启用持久化以避免重复计算向量：

```yaml
spring:
  ai:
    vectorstore:
      persist:
        enabled: true
        path: ./data/vectorstore.json
```

注意：当前版本的 `SimpleVectorStore` 可能不完全支持持久化，这是为未来版本预留的接口。

### 4. 使用不同的向量存储

除了 `SimpleVectorStore`，还可以使用其他向量存储：

```java
// Pinecone
@Bean
VectorStore pineconeVectorStore(EmbeddingModel embeddingModel) {
    PineconeVectorStoreConfig config = PineconeVectorStoreConfig.builder()
        .withApiKey(pineconeApiKey)
        .withEnvironment(pineconeEnvironment)
        .withProjectId(pineconeProjectId)
        .withIndexName(pineconeIndexName)
        .build();
    
    return new PineconeVectorStore(config, embeddingModel);
}

// Weaviate
@Bean
VectorStore weaviateVectorStore(EmbeddingModel embeddingModel) {
    WeaviateVectorStoreConfig config = WeaviateVectorStoreConfig.builder()
        .withScheme("http")
        .withHost("localhost:8080")
        .build();
    
    return new WeaviateVectorStore(config, embeddingModel);
}
```

## 性能优化建议

### 1. 开发环境

```yaml
spring:
  cache:
    type: simple
  ai:
    document:
      chunk-size: 800
      chunk-overlap: 100
```

### 2. 生产环境

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
  ai:
    document:
      chunk-size: 1000
      chunk-overlap: 150
    vectorstore:
      persist:
        enabled: true
```

### 3. 大规模文档

如果有大量文档（>1000个文件）：

```yaml
spring:
  ai:
    document:
      chunk-size: 1200
      max-num-chunks: 50000
```

并考虑使用专业的向量数据库（Pinecone、Weaviate、Milvus）。

## 监控和调试

### 1. 启用详细日志

```yaml
logging:
  level:
    com.wly.ai_agent_plus.RAG: DEBUG
```

### 2. 查看加载统计

```java
@Autowired
private LoveAppDocumentLoader documentLoader;

public void printStatistics() {
    List<Document> documents = documentLoader.loadmarkdowns();
    
    // 统计文件数量
    long fileCount = documents.stream()
        .map(doc -> doc.getMetadata().get("filename"))
        .distinct()
        .count();
    
    // 统计平均切片大小
    double avgLength = documents.stream()
        .mapToInt(doc -> doc.getText().length())
        .average()
        .orElse(0);
    
    System.out.println("文件数量: " + fileCount);
    System.out.println("总切片数: " + documents.size());
    System.out.println("平均切片长度: " + avgLength);
}
```

### 3. 测试缓存效果

运行测试类中的 `testCachePerformance` 方法：

```bash
mvn test -Dtest=LoveAppDocumentLoaderTest#testCachePerformance
```

## 常见问题

### Q1: 文档没有被加载？

**检查清单**:
1. 确认文档位置配置正确
2. 确认文件扩展名为 `.md`
3. 查看日志中的警告信息
4. 确认文件编码为 UTF-8

### Q2: 切片太大或太小？

**解决方案**:
调整 `chunk-size` 参数：
- 切片太大：减小 `chunk-size`（如 600）
- 切片太小：增大 `chunk-size`（如 1000）

### Q3: 检索结果不准确？

**优化建议**:
1. 增加 `chunk-overlap`（如 150-200）
2. 调整 `chunk-size` 以平衡精度和上下文
3. 检查文档质量和格式
4. 考虑使用更好的 Embedding 模型

### Q4: 启动速度慢？

**解决方案**:
1. 启用缓存（`@EnableCaching`）
2. 考虑使用向量存储持久化
3. 减少文档数量或切片大小
4. 使用更快的 Embedding 模型

### Q5: 内存占用高？

**优化方案**:
1. 减小 `chunk-size`
2. 限制 `max-num-chunks`
3. 使用外部向量数据库
4. 定期清理缓存

## 最佳实践

### 1. 文档组织

```
src/main/resources/document/
├── category1/
│   ├── doc1.md
│   └── doc2.md
├── category2/
│   ├── doc3.md
│   └── doc4.md
└── general.md
```

配置多个位置：

```yaml
spring:
  ai:
    document:
      location: classpath:document/**/*.md  # 递归搜索
```

### 2. 元数据利用

在检索时利用元数据过滤：

```java
// 只检索特定文件的内容
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(query)
        .withTopK(5)
        .withFilterExpression("filename == 'love-tips.md'")
);

// 只检索特定标题层级
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(query)
        .withTopK(5)
        .withFilterExpression("level <= 2")
);
```

### 3. 定期更新

如果文档经常更新，考虑添加更新机制：

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void refreshDocuments() {
    cacheManager.getCache("markdownDocuments").clear();
    documentLoader.loadmarkdowns();
}
```

## 总结

优化后的文档加载器提供了：
- ✅ 标准化的 Spring AI 接口
- ✅ 灵活的配置选项
- ✅ 高性能的并行处理
- ✅ 智能的缓存机制
- ✅ 丰富的元数据支持
- ✅ 完善的错误处理

根据你的具体需求调整配置，即可获得最佳的 RAG 性能！
