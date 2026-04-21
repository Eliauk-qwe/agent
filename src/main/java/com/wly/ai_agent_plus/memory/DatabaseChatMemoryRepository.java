package com.wly.ai_agent_plus.memory;

import com.wly.ai_agent_plus.entity.ChatMessageEntity;
import com.wly.ai_agent_plus.mapper.ChatMessageMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于数据库的对话记忆存储实现
 */
@Component
public class DatabaseChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageMapper chatMessageMapper;

    public DatabaseChatMemoryRepository(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<String> findConversationIds() {
        return chatMessageMapper.findAllConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return chatMessageMapper.findByConversationId(conversationId)
                .stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    /**
     * 替换指定会话的所有消息（先删后存）
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        chatMessageMapper.deleteByConversationId(conversationId);
        for (Message message : messages) {
            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setConversationId(conversationId);
            entity.setRole(getRoleName(message));
            entity.setContent(message.getText());
            chatMessageMapper.insert(entity);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatMessageMapper.deleteByConversationId(conversationId);
    }

    private String getRoleName(Message message) {
        if (message instanceof UserMessage) return "user";
        if (message instanceof AssistantMessage) return "assistant";
        return "system";
    }

    private Message toMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case "assistant" -> new AssistantMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }
}
