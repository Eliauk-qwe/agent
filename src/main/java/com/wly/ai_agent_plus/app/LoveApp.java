package com.wly.ai_agent_plus.app;

import com.wly.ai_agent_plus.RAG.LoveAppContextualQueryAugmenterFactory;
import com.wly.ai_agent_plus.RAG.QueryRewriter;
import com.wly.ai_agent_plus.advisor.SafeGuardAdvisor;
import com.wly.ai_agent_plus.advisor.myadvisor;
import com.wly.ai_agent_plus.memory.DatabaseChatMemoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

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

        // 对话记忆，保存多轮对话的历史。
        private MessageWindowChatMemory chatMemory;

        private String systemPrompt;
        
        private final ChatClient.Builder chatClientBuilder;
        private final DatabaseChatMemoryRepository databaseRepository;

        // 注入向量存储
        @Autowired
        private VectorStore loveAppVectorStore;

        // 注入查询重写组件
        @Autowired
        private QueryRewriter queryRewriter;

        
        
        public LoveApp(ChatClient.Builder chatClientBuilder,
                        DatabaseChatMemoryRepository databaseRepository) {

                this.chatClientBuilder = chatClientBuilder;
                this.databaseRepository = databaseRepository;
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
                SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
                this.systemPrompt = systemPromptTemplate.render();



                // 基于数据库的对话记忆，最多保留10条消息
                this.chatMemory = MessageWindowChatMemory.builder()
                        //改变
                        //.chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .chatMemoryRepository(databaseRepository)
                        .maxMessages(10)
                        .build();

                this.chatClient = chatClientBuilder
                        .defaultSystem(systemPrompt)
                        .defaultAdvisors(
                                new myadvisor(),
                                new SafeGuardAdvisor()
                                //new ReReadingAdvisor()
                        )
                        .build();
        }

    /**
     * 多轮对话，chatId 用于区分不同会话，userId 用于权限校验
     */
    public void chat(String message, String chatId) {
        chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(chatId).build())

                )
                .call()
                .content();
    }
    /**
     * 结构化输出
     */

    record LoveReport(String title, List<String> suggestions) {
    }

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








}



