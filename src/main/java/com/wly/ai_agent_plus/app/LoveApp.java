package com.wly.ai_agent_plus.app;

import com.wly.ai_agent_plus.advisor.SafeGuardAdvisor;
import com.wly.ai_agent_plus.advisor.myadvisor;
import com.wly.ai_agent_plus.memory.DatabaseChatMemoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
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
        
        private ChatClient chatClient;

        // 对话记忆，跨请求保持上下文
        private MessageWindowChatMemory chatMemory;

        private String systemPrompt;
        
        private final ChatClient.Builder chatClientBuilder;
        private final DatabaseChatMemoryRepository databaseRepository;
        
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




}



