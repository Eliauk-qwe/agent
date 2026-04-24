# 超时问题修复总结

## 问题描述
测试 `LoveAppTest.doChatWithRag()` 时出现 `SocketTimeoutException: timeout` 错误。

## 根本原因分析

### 1. 网络连接正常 ✅
```bash
curl -v https://dashscope.aliyuncs.com/...
# 响应时间: 4ms
# 结论: 网络畅通,不是网络问题
```

### 2. 真正的问题: QueryRewriter 使用默认超时 ❌

RAG 流程包含两次 LLM 调用:
```
用户问题
  ↓
[1] QueryRewriter.rewrite()  ← 使用默认 60 秒超时 (问题所在!)
  ↓
向量检索
  ↓
[2] LoveApp.chatClient.call() ← 使用 application.yml 配置的 180 秒超时
  ↓
最终回答
```

**问题**: `QueryRewriter` 创建的 `ChatClient` 没有继承 `application.yml` 的超时配置!

## 修复方案

### 修改 1: 创建 OkHttp 客户端配置
**文件**: `src/main/java/com/wly/ai_agent_plus/config/DashScopeClientConfig.java`

```java
@Configuration
public class DashScopeClientConfig {
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(180))    // 3 分钟
                .writeTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(200))
                .build();
    }
}
```

### 修改 2: 更新 application.yml
```yaml
spring:
  ai:
    dashscope:
      read-timeout: 180  # 从 120 增加到 180 秒
    retry:
      max-attempts: 2    # 从 1 增加到 2,允许重试
      on-client-errors: false
```

### 修改 3: 修复 QueryRewriter (关键!)
**文件**: `src/main/java/com/wly/ai_agent_plus/RAG/QueryRewriter.java`

**修改前**:
```java
public QueryRewriter(ChatModel dashscopeChatModel) {
    ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
    // ❌ 这个 builder 使用默认超时配置
    queryTransformer = RewriteQueryTransformer.builder()
            .chatClientBuilder(builder)
            .build();
}
```

**修改后**:
```java
public QueryRewriter(ChatClient.Builder chatClientBuilder) {
    // ✅ 直接注入 ChatClient.Builder,继承 application.yml 的超时配置
    queryTransformer = RewriteQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .build();
}
```

## 测试验证

运行测试:
```bash
mvn test -Dtest=LoveAppTest#doChatWithRag
```

预期结果:
- ✅ 查询重写步骤不再超时
- ✅ 整个 RAG 流程在 180 秒内完成
- ✅ 测试通过

## 为什么这样修复有效?

1. **统一超时配置**: `QueryRewriter` 和 `LoveApp` 现在使用同一个 `ChatClient.Builder`,共享超时配置
2. **三层保护**:
   - OkHttp 层: 180 秒读取超时
   - Spring AI 层: 180 秒读取超时
   - 整体调用: 200 秒总超时
3. **允许重试**: 网络波动时可以重试一次

## 如果还是超时怎么办?

### 临时方案: 禁用查询重写
在 `LoveApp.chatwithrag()` 中:
```java
// String rewrittenMessage = queryRewriter.rewrite(message);
String rewrittenMessage = message;  // 直接使用原始问题
```

### 优化方案: 减少检索数量
```java
.topK(3)  // 从 5 改为 3
```

### 监控方案: 添加日志
```java
long start = System.currentTimeMillis();
String rewrittenMessage = queryRewriter.rewrite(message);
log.info("查询重写耗时: {}ms", System.currentTimeMillis() - start);
```
