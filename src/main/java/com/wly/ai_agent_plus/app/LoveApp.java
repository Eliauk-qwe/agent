package com.wly.ai_agent_plus.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoveApp {

    private final ChatClient chatClient;

    // 对话记忆，跨请求保持上下文
    private final MessageWindowChatMemory chatMemory;

    public LoveApp(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader) throws IOException {
        // 读取系统提示词文件
        Resource resource = resourceLoader.getResource("classpath:prompts/love-consultant.md");
        String systemPrompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        // 基于内存的对话记忆，最多保留10条消息
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 多轮对话，chatId 用于区分不同会话
     */
    public String chat(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(chatId)
                        .build())
                .call()
                .content();
    }
}
