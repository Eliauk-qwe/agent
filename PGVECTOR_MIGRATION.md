# PGVector 向量存储迁移说明

## 配置文件变更

### 当前配置（激活中）
**文件**: `src/main/java/com/wly/ai_agent_plus/RAG/SimpleVectorStoreConfig.java`

**特点**:
- ✅ 内存存储，配置简单
- ✅ 无需额外数据库
- ✅ 适合开发测试
- ❌ 应用重启后数据丢失
- ❌ 每次启动都要调用 36 次 embedding API（约 15 秒）

### 备选配置 1（已注释）
**文件**: `src/main/java/com/wly/ai_agent_plus/RAG/LoveAppVectorStoreConfig.java`

**改进**:
1. ✅ 正确的向量维度（1024 维，匹配通义千问 embedding 输出）
2. ✅ 自动创建 pgvector 扩展（vector, hstore, uuid-ossp）
3. ✅ 手动创建表结构，确保与 Spring AI 兼容
4. ✅ 创建 HNSW 索引提升查询性能
5. ✅ 集成文档加载器（LoveAppDocumentLoader）
6. ✅ 检查数据是否已存在，避免重复加载
7. ✅ 逐个文档添加，避免 DashScope API 批次限制
8. ✅ 进度日志（每 10 个文档输出一次）
9. ✅ 数据持久化，重启不丢失

### 备选配置 2（已注释，不推荐）
**文件**: `src/main/java/com/wly/ai_agent_plus/RAG/PgVectorVectorStoreConfig.java`

**问题**:
1. ❌ 向量维度设置错误（1536 应该是 1024）
2. ❌ 没有文档加载逻辑
3. ❌ 没有检查数据是否已存在，每次启动都会重复加载
4. ❌ 没有创建 pgvector 扩展
5. ❌ 没有批处理优化

## 数据库配置

### MySQL（对话记忆）
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_agent
    username: root
    password: 123456
```

### PostgreSQL（向量存储）
```yaml
spring:
  pgvector:
    datasource:
      url: jdbc:postgresql://your-host/agent
      username: www
      password: your-password
```

## 向量维度说明

**重要**: 通义千问 text-embedding-v1/v2 输出 **1024 维**向量，不是 OpenAI 的 1536 维。

配置位置:
- `application.yml`: `spring.ai.vectorstore.pgvector.dimensions: 1024`
- `LoveAppVectorStoreConfig.java`: `@Value("${spring.ai.vectorstore.pgvector.dimensions:1024}")`

## 启动流程

### 第一次启动
1. 连接 PostgreSQL
2. 创建 pgvector 扩展
3. 删除旧表（如果存在）
4. 创建新表（1024 维向量）
5. 创建 HNSW 索引
6. 加载 5 个 Markdown 文档
7. 切分成 36 个文档片段
8. 调用 36 次 embedding API（约 15 秒）
9. 逐个插入 PostgreSQL
10. 应用启动完成

### 后续重启
1. 连接 PostgreSQL
2. 检查向量表是否有数据
3. **跳过文档加载**（数据已存在）
4. 应用启动完成（约 5 秒）

## 性能对比

| 指标 | SimpleVectorStore（当前） | PGVector（备选） |
|------|---------------------------|------------------|
| 存储位置 | JVM 内存 | PostgreSQL |
| 持久化 | ❌ 否 | ✅ 是 |
| 重启后 | 重新加载（36 次 API） | 直接读取 |
| 启动时间 | 每次 15 秒 | 首次 20 秒，后续 5 秒 |
| Token 消耗 | 每次启动都消耗 | 仅首次消耗 |
| 查询性能 | 内存快速 | HNSW 索引优化 |
| 配置复杂度 | ⭐ 简单 | ⭐⭐⭐ 需要 PostgreSQL |
| 适用场景 | 开发测试 | 生产环境 |

## 如何切换回旧配置

### 方案 A: 使用 SimpleVectorStore（内存存储）- 当前激活

**特点**:
- ✅ 配置简单，无需 PostgreSQL
- ✅ 适合开发测试
- ❌ 应用重启后数据丢失
- ❌ 每次启动都要调用 36 次 embedding API

**配置文件**: `SimpleVectorStoreConfig.java`

**状态**: ✅ 当前激活

### 方案 B: 使用 PGVector（PostgreSQL 持久化）- 已注释

**特点**:
- ✅ 数据持久化，重启不丢失
- ✅ 仅首次调用 API，后续直接读取
- ✅ 适合生产环境
- ❌ 需要 PostgreSQL + pgvector 扩展

**配置文件**: `LoveAppVectorStoreConfig.java`

**状态**: ⏸️ 已暂时注释

### 切换步骤

#### 从 SimpleVectorStore 切换到 PGVector:

1. 注释掉 `SimpleVectorStoreConfig.java`:
   ```java
   // @Configuration
   public class SimpleVectorStoreConfig {
       // @Bean
       public VectorStore loveAppVectorStore(...) {
   ```

2. 取消注释 `LoveAppVectorStoreConfig.java`:
   ```java
   @Configuration  // 取消注释
   public class LoveAppVectorStoreConfig {
       @Bean  // 取消注释
       VectorStore loveAppVectorStore(...) {
   ```

3. 确保 PostgreSQL 配置正确（`application.yml`）

4. 重新编译运行

#### 从 PGVector 切换到 SimpleVectorStore:

1. 注释掉 `LoveAppVectorStoreConfig.java`:
   ```java
   // @Configuration
   public class LoveAppVectorStoreConfig {
       // @Bean
       VectorStore loveAppVectorStore(...) {
   ```

2. 取消注释 `SimpleVectorStoreConfig.java`:
   ```java
   @Configuration  // 取消注释
   public class SimpleVectorStoreConfig {
       @Bean  // 取消注释
       public VectorStore loveAppVectorStore(...) {
   ```

3. 重新编译运行

### 方案 C: 使用旧的 PgVectorVectorStoreConfig（不推荐）

如果需要使用最初的配置（不推荐，有维度错误）:

1. 注释掉当前激活的配置
2. 取消注释 `PgVectorVectorStoreConfig.java` 的 `@Configuration` 和 `@Bean`
3. 修改维度为 1024: `.dimensions(1024)`
4. 重新编译运行

## 注意事项

1. **PostgreSQL 必须安装 pgvector 扩展**
2. **数据库用户需要创建扩展的权限**
3. **向量维度必须与 embedding 模型匹配**（通义千问 = 1024）
4. **首次启动会调用 36 次 API**，确保 API key 有效且有足够配额
5. **如果修改了文档内容**，需要删除 PostgreSQL 中的 `vector_store` 表重新加载

## 验证配置成功

### SimpleVectorStore 启动日志（当前）
```
✅ 初始化 SimpleVectorStore（内存向量存储）
✅ 开始加载文档到 SimpleVectorStore
✅ 开始加载 Markdown 文档
✅ 找到 5 个 Markdown 文件
✅ 成功加载 36 个文档片段
✅ 准备添加 36 个文档到 SimpleVectorStore
✅ 文档添加完成，已存储到内存（应用重启后会丢失）
✅ Started AiAgentPlusApplication
```

### PGVector 启动日志（备选）
```
✅ PostgreSQL 扩展检查完成
✅ 已清理旧的 vector_store 表
✅ 成功创建 vector_store 表
✅ 成功创建向量索引
✅ 成功加载 36 个文档片段
✅ 已添加 10/36 个文档
✅ 已添加 20/36 个文档
✅ 已添加 30/36 个文档
✅ 已添加 36/36 个文档
✅ 文档添加完成，已持久化到 PostgreSQL
✅ Started AiAgentPlusApplication
```

## 故障排查

### 问题: ERROR: expected 1536 dimensions, not 1024
**原因**: 向量维度配置错误  
**解决**: 确认 `application.yml` 和代码中都设置为 1024

### 问题: ERROR: relation "public.vector_store" does not exist
**原因**: 表创建失败  
**解决**: 检查 PostgreSQL 连接和权限

### 问题: ERROR: extension "vector" does not exist
**原因**: PostgreSQL 未安装 pgvector 扩展  
**解决**: 安装 pgvector 扩展或授予创建扩展权限

### 问题: batch size is invalid, it should not be larger than 10
**原因**: DashScope API 批次限制  
**解决**: 已在新配置中解决（逐个文档添加）
