package com.wly.ai_agent_plus.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.wly.ai_agent_plus.RAG.LoveAppContextualQueryAugmenterFactory;
import com.wly.ai_agent_plus.RAG.QueryRewriter;
import com.wly.ai_agent_plus.advisor.SafeGuardAdvisor;
import com.wly.ai_agent_plus.advisor.myadvisor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * 恋爱顾问 Agent 的主入口。
 *
 * 这个类把 Spring AI 的 ChatClient、对话记忆、RAG 检索、本地工具和 MCP 工具串起来，
 * 对外提供不同的对话能力。当前更像一个应用门面：调用方只需要传入用户消息和 chatId，
 * 具体走普通对话、RAG 对话、工具调用还是 MCP 调用由对应方法决定。
 */
@Component
@Slf4j
public class LoveApp {
        // 从类路径资源加载系统提示模板
        @Value("classpath:/prompts/love-consultant.md")
        private Resource systemResource;

        // RAG 提示词模板（定义如何把检索到的文档拼入 prompt）
        @Value("classpath:/prompts/rag-prompt-template.st")
        private Resource ragPromptResource;
        
        private ChatClient chatClient;

        // 对话记忆，默认保存在当前 JVM 内存中；服务重启后历史会清空。
        private MessageWindowChatMemory chatMemory;

        // 渲染后的系统提示词，会在初始化后被复用，避免每次请求重复读取模板文件。
        private String systemPrompt;
        
        // ChatClient.Builder 由 Spring AI 自动配置，包含模型、连接信息等基础能力。
        private final ChatClient.Builder chatClientBuilder;

        // 注入向量存储
        @Autowired
        private VectorStore loveAppVectorStore;

        // 注入查询重写组件
        @Autowired
        private QueryRewriter queryRewriter;

        
        
        public LoveApp(ChatClient.Builder chatClientBuilder) {
                this.chatClientBuilder = chatClientBuilder;
        }
        
        @PostConstruct
        public void init() throws IOException {
//     public LoveApp(ChatClient.Builder chatClientBuilder, 
//                    ResourceLoader resourceLoader,
//                    DatabaseChatMemoryRepository databaseRepository) throws IOException {
        // 读取系统提示词文件
//        Resource resource = resourceLoader.getResource("classpath:prompts/love-consultant.md");
//        systemPrompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);




                // 使用 SystemPromptTemplate 读取文件
                // 注意：这里在 Bean 初始化阶段完成模板渲染，后续每次对话直接复用 systemPrompt。
                SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
                this.systemPrompt = systemPromptTemplate.render();



                // 基于内存的对话记忆，最多保留10条消息。
                // MessageWindowChatMemory 会控制上下文窗口大小，避免历史消息无限增长导致 token 过多。
                this.chatMemory = MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(10)
                        .build();

                this.chatClient = chatClientBuilder
                        .defaultSystem(systemPrompt)
                        .defaultOptions(DashScopeChatOptions.builder()
                                .enableThinking(false)  // Qwen3 系列必须关闭思考模式，否则非流式调用报错
                                .build())
                        // 默认 Advisor 会作用在所有通过该 chatClient 发起的请求上。
                        .defaultAdvisors(
                                new myadvisor(),
                                new SafeGuardAdvisor()
                        )
                        .build();
        }

    /**
     * 多轮对话，chatId 用于区分不同会话，userId 用于权限校验
     *
     * chatId 是会话隔离的关键：同一个 chatId 会共享历史记忆，不同 chatId 互不影响。
     */
    public String chat(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatId).build())

                )
                .call()
                .content();
    }

    /**
     * 多轮对话的流式输出版本。
     */
    public Flux<String> chatStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatId).build())
                )
                .stream()
                .content();
    }

    /**
     * 使用 RAG 知识库的流式对话版本。
     */
    public Flux<String> chatStreamWithRag(String message, String chatId) {
        return Flux.defer(() -> {
            log.info("=== RAG 流式对话开始 ===");
            long startTime = System.currentTimeMillis();

            long step1Start = System.currentTimeMillis();
            String rewrittenMessage = queryRewriter.rewrite(message);
            long step1Time = System.currentTimeMillis() - step1Start;
            log.info("RAG 步骤1完成: 查询重写耗时 {}ms, [{}] -> [{}]", step1Time, message, rewrittenMessage);

            long step2Start = System.currentTimeMillis();
            List<Document> documents = loveAppVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(rewrittenMessage)
                            .topK(5)
                            .build()
            );
            long step2Time = System.currentTimeMillis() - step2Start;
            log.info("RAG 步骤2完成: 向量检索耗时 {}ms, 检索到 {} 个相关文档", step2Time, documents.size());

            long step3Start = System.currentTimeMillis();
            ContextualQueryAugmenter augmenter = LoveAppContextualQueryAugmenterFactory.createInstance(ragPromptResource);
            Query augmentedQuery = augmenter.augment(new Query(rewrittenMessage), documents);
            long step3Time = System.currentTimeMillis() - step3Start;
            log.info("RAG 步骤3完成: 上下文增强耗时 {}ms", step3Time);

            String combinedSystemPrompt = systemPrompt + "\n\n## 知识库参考\n" + augmentedQuery.text();

            Flux<String> processStream = Flux.just(
                    "[[PROCESS]]Step 1: 查询重写\n原始问题: " + message + "\n改写后: " + rewrittenMessage + "\n耗时: " + step1Time + "ms",
                    "[[PROCESS]]Step 2: 向量知识库检索\n检索到 " + documents.size() + " 个相关文档。\n耗时: " + step2Time + "ms",
                    "[[PROCESS]]Step 3: 上下文增强\n将知识库内容注入恋爱大师提示词。\n耗时: " + step3Time + "ms",
                    "[[PROCESS]]Step 4: MCP 工具准备\n" + (toolCallbackProvider == null ? "MCP 未启用或没有可用工具，本次仅使用知识库。" : "已接入 MCP 工具，可由模型按需调用。"),
                    "[[PROCESS]]Step 5: 调用大模型\n基于知识库参考生成流式回复。"
            );

            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .system(combinedSystemPrompt)
                    .user(message)
                    .advisors(spec -> spec
                            .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatId).build())
                    );

            if (toolCallbackProvider != null) {
                requestSpec.toolCallbacks(toolCallbackProvider);
            }

            Flux<String> answerStream = requestSpec
                    .stream()
                    .content()
                    .filter(content -> content != null && !content.isEmpty())
                    .map(content -> "[[ANSWER]]" + content)
                    .doFinally(signalType -> {
                        long totalTime = System.currentTimeMillis() - startTime;
                        log.info("=== RAG 流式对话结束，signal={}, 总耗时 {}ms ===", signalType, totalTime);
                    });

            return Flux.concat(processStream, answerStream);
        });
    }
    /**
     * 结构化输出
     */

    /**
     * 结构化返回结果。
     *
     * Spring AI 会根据 record 字段把模型输出映射为 Java 对象，
     * 适合报告、表单、摘要等需要固定结构的场景。
     */
    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 生成恋爱咨询报告。
     *
     * 与普通 chat 不同，这里通过 entity(LoveReport.class) 要求模型输出可映射的结构化结果。
     */
    public LoveReport dowithreport(String message,String chatID){
        LoveReport loveReport = chatClient.prompt()
                .system(systemPrompt+ "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatID).build())

                )
                .call()
                .entity(LoveReport.class);
        log.info("恋爱报告: {}", loveReport);
        return loveReport;
    }

    /**
     * 带 RAG + 查询重写 + 空结果处理的对话方法
     *
     * 执行顺序：
     *   1. QueryRewriter：把口语化问题改写成更适合检索的形式
     *   2. VectorStore.similaritySearch()：用改写后的问题检索向量库
     *   3. ContextualQueryAugmenter：
     *        → 有结果：把文档内容拼成 system 补充提示
     *        → 无结果：返回固定提示语（"只能回答恋爱相关问题"）
     *   4. LLM：基于注入的文档内容生成回答
     *
     * 关键设计：
     *   augmentedQuery.text() 作为 system() 传入，而不是 user()
     *   这样 MessageChatMemoryAdvisor 存入数据库的是原始用户问题 message，
     *   而不是几千字的知识库内容，避免下一轮对话 token 爆炸。
     */
    public void chatwithrag(String message, String chatId) {
        log.info("=== RAG 对话开始 ===");
        long startTime = System.currentTimeMillis();
        
        // 第一步：查询重写
        log.info("步骤1: 开始查询重写...");
        long step1Start = System.currentTimeMillis();
        String rewrittenMessage = queryRewriter.rewrite(message);
        long step1Time = System.currentTimeMillis() - step1Start;
        log.info("步骤1完成: 查询重写耗时 {}ms, [{}] -> [{}]", step1Time, message, rewrittenMessage);

        // 第二步：向量检索
        log.info("步骤2: 开始向量检索...");
        long step2Start = System.currentTimeMillis();
        List<Document> documents = loveAppVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(rewrittenMessage)
                        .topK(5)
                        .build()
        );
        long step2Time = System.currentTimeMillis() - step2Start;
        log.info("步骤2完成: 向量检索耗时 {}ms, 检索到 {} 个相关文档", step2Time, documents.size());

        // 第三步：ContextualQueryAugmenter 处理
        log.info("步骤3: 开始上下文增强...");
        long step3Start = System.currentTimeMillis();
        ContextualQueryAugmenter augmenter = LoveAppContextualQueryAugmenterFactory.createInstance(ragPromptResource);
        Query augmentedQuery = augmenter.augment(new Query(rewrittenMessage), documents);
        long step3Time = System.currentTimeMillis() - step3Start;
        log.info("步骤3完成: 上下文增强耗时 {}ms", step3Time);

        // 第四步：调用 LLM
        log.info("步骤4: 开始调用 LLM 生成最终回答...");
        long step4Start = System.currentTimeMillis();
        
        // 关键修复：将 RAG 知识库内容追加到系统提示词,而不是替换
        // systemPrompt: 恋爱大师的角色定义和行为准则
        // augmentedQuery.text(): RAG 检索到的相关知识库内容
        String combinedSystemPrompt = systemPrompt + "\n\n## 知识库参考\n" + augmentedQuery.text();
        
        String response = chatClient.prompt()
                .system(combinedSystemPrompt)  // ✅ 使用合并后的系统提示词
                .user(message)                  // 原始问题作为 user，存入对话记忆
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatId).build())
                )
                .call()
                .content();
        long step4Time = System.currentTimeMillis() - step4Start;
        log.info("步骤4完成: LLM 生成耗时 {}ms", step4Time);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== RAG 对话完成，总耗时 {}ms ===", totalTime);
        log.info("回答: {}", response);
    }





        // Spring 容器中注册的本地 @Tool 工具集合，例如文件、网页抓取、PDF 生成等。
        @jakarta.annotation.Resource
        private ToolCallback[] allTools;

        /**
         * 带本地工具调用能力的对话。
         *
         * tools(allTools) 会把本地 Java 方法暴露给模型，由模型决定是否调用具体工具。
         */
        public String doChatWithTools(String message, String chatID) {
        ChatResponse response = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatID).build())

                )
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
        }



        // MCP 工具回调提供者，负责把外部 MCP Server 暴露出的工具接入 ChatClient。
        @Autowired(required = false)
        private ToolCallbackProvider toolCallbackProvider;

        /**
         * 带 MCP 工具调用能力的对话。
         *
         * MCP 工具来自外部进程或服务，所以这里使用 toolCallbacks(toolCallbackProvider)，
         * 与本地 @Tool 注解方法的 tools(allTools) 是两套不同的接入方式。
         */
        public void dochatwithmcp(String message, String chatID) {
            if (toolCallbackProvider == null) {
                throw new IllegalStateException("MCP client is disabled or no MCP tool callback provider is available");
            }
            ChatResponse response = chatClient.prompt()
                    .user(message)
                    .advisors(spec -> spec.advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatID).build()))
                    .toolCallbacks(toolCallbackProvider)  // MCP 工具用 toolCallbacks()，不是 tools()
                    .call()
                    .chatResponse();

        }









}
