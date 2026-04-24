# 向量存储配置切换指南

## 当前状态

✅ **SimpleVectorStore** - 激活中  
⏸️ **LoveAppVectorStoreConfig (PGVector)** - 已注释  
⏸️ **PgVectorVectorStoreConfig (旧 PGVector)** - 已注释

## 三种配置对比

| 配置 | 文件 | 状态 | 存储位置 | 持久化 | 适用场景 |
|------|------|------|----------|--------|----------|
| SimpleVectorStore | `SimpleVectorStoreConfig.java` | ✅ 激活 | JVM 内存 | ❌ | 开发测试 |
| PGVector（新） | `LoveAppVectorStoreConfig.java` | ⏸️ 注释 | PostgreSQL | ✅ | 生产环境 |
| PGVector（旧） | `PgVectorVectorStoreConfig.java` | ⏸️ 注释 | PostgreSQL | ✅ | 不推荐 |

## 快速切换

### 当前使用：SimpleVectorStore

**优点**:
- 配置简单，无需额外数据库
- 启动快速（首次 15 秒）
- 适合开发调试

**缺点**:
- 每次重启都要调用 36 次 embedding API
- 数据不持久化
- 消耗 API token

### 切换到 PGVector（推荐生产环境）

**步骤**:

1. **注释 SimpleVectorStore**:
   ```java
   // src/main/java/com/wly/ai_agent_plus/RAG/SimpleVectorStoreConfig.java
   // @Configuration  // 添加注释
   public class SimpleVectorStoreConfig {
       // @Bean  // 添加注释
       public VectorStore loveAppVectorStore(...) {
   ```

2. **启用 PGVector**:
   ```java
   // src/main/java/com/wly/ai_agent_plus/RAG/LoveAppVectorStoreConfig.java
   @Configuration  // 取消注释
   @Slf4j
   public class LoveAppVectorStoreConfig {
       @Bean  // 取消注释
       VectorStore loveAppVectorStore(...) {
   ```

3. **确认 PostgreSQL 配置**:
   ```yaml
   # application.yml
   spring:
     pgvector:
       datasource:
         url: jdbc:postgresql://your-host/agent
         username: www
         password: your-password
     ai:
       vectorstore:
         pgvector:
           dimensions: 1024  # 通义千问维度
   ```

4. **重新编译运行**:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

**优点**:
- 数据持久化，重启不丢失
- 仅首次调用 API（约 20 秒）
- 后续启动快速（约 5 秒）
- 节省 API token

**缺点**:
- 需要 PostgreSQL + pgvector 扩展
- 配置相对复杂

## 配置文件说明

### SimpleVectorStoreConfig.java（当前激活）

```java
@Configuration  // ✅ 激活
@Slf4j
public class SimpleVectorStoreConfig {
    @Bean  // ✅ 激活
    public VectorStore loveAppVectorStore(EmbeddingModel embeddingModel) {
        // 创建内存向量存储
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        
        // 加载文档（每次启动都执行）
        List<Document> documents = loveAppDocumentLoader.loadmarkdowns();
        vectorStore.add(documents);  // 调用 36 次 embedding API
        
        return vectorStore;
    }
}
```

### LoveAppVectorStoreConfig.java（已注释）

```java
// @Configuration  // ⏸️ 已注释
@Slf4j
public class LoveAppVectorStoreConfig {
    // @Bean  // ⏸️ 已注释
    VectorStore loveAppVectorStore(@NonNull EmbeddingModel embeddingModel) {
        // 1. 创建 pgvector 扩展
        // 2. 创建表和索引
        // 3. 检查数据是否已存在
        // 4. 如果不存在，加载文档并持久化
        // 5. 如果已存在，直接使用
    }
}
```

### PgVectorVectorStoreConfig.java（已注释，不推荐）

```java
// @Configuration  // ⏸️ 已注释
public class PgVectorVectorStoreConfig {
    // @Bean  // ⏸️ 已注释
    public VectorStore pgVectorVectorStore(...) {
        // 旧配置，有维度错误问题
        // 不推荐使用
    }
}
```

## 启动时间对比

| 场景 | SimpleVectorStore | PGVector |
|------|-------------------|----------|
| 首次启动 | 15 秒 | 20 秒 |
| 第二次启动 | 15 秒 | 5 秒 |
| 第三次启动 | 15 秒 | 5 秒 |
| ... | 15 秒 | 5 秒 |

**结论**: 
- 开发测试：使用 SimpleVectorStore（当前配置）
- 生产环境：使用 PGVector（需要切换）

## 常见问题

### Q: 为什么不直接使用 PGVector？
A: 开发测试阶段，SimpleVectorStore 更简单，无需配置 PostgreSQL。

### Q: 如何知道当前使用哪个配置？
A: 查看启动日志：
- SimpleVectorStore: `初始化 SimpleVectorStore（内存向量存储）`
- PGVector: `初始化 PGVector 向量存储，向量维度: 1024`

### Q: 切换配置需要修改代码吗？
A: 不需要。只需要注释/取消注释 `@Configuration` 和 `@Bean` 注解。

### Q: 两个配置可以同时激活吗？
A: 不可以。会导致 Bean 冲突。同一时间只能激活一个。

### Q: 如何验证切换成功？
A: 查看启动日志，确认使用的是目标配置。

## 推荐配置

- **开发环境**: SimpleVectorStore（当前）
- **测试环境**: SimpleVectorStore 或 PGVector
- **生产环境**: PGVector（需要切换）

## 文件清单

```
src/main/java/com/wly/ai_agent_plus/RAG/
├── LoveAppDocumentLoader.java          # 文档加载器（共用）
├── SimpleVectorStoreConfig.java        # ✅ 当前激活
├── LoveAppVectorStoreConfig.java       # ⏸️ 已注释（推荐生产）
└── PgVectorVectorStoreConfig.java      # ⏸️ 已注释（不推荐）
```

## 下一步

如果需要切换到 PGVector：
1. 确保 PostgreSQL 已安装 pgvector 扩展
2. 配置 `application.yml` 中的 PostgreSQL 连接
3. 按照上述步骤切换配置
4. 重新编译运行
5. 验证启动日志
