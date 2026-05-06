package com.wly.ai_agent_plus.memory;

import com.wly.ai_agent_plus.entity.ChatMessageEntity;
import com.wly.ai_agent_plus.mapper.ChatMessageMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 MySQL 数据库的对话记忆存储实现
 *
 * 调用关系：
 *   MessageWindowChatMemory（Spring AI 框架）
 *       → DatabaseChatMemoryRepository（当前类，你自己实现的）
 *           → ChatMessageMapper（MyBatis，执行 SQL）
 *               → MySQL 数据库 chat_message 表
 *
 * 为什么这样写：
 *   Spring AI 定义了 ChatMemoryRepository 接口，规定了读写对话记忆必须实现哪些方法。
 *   默认只提供了内存实现（InMemoryChatMemoryRepository），应用重启后记忆就丢失了。
 *   这里自己实现这个接口，把数据存到 MySQL，这样重启后历史对话依然存在。
 */
@Component
@ConditionalOnProperty(name = "app.chat-memory.type", havingValue = "database")
public class DatabaseChatMemoryRepository implements ChatMemoryRepository {

    // 注入 MyBatis Mapper，用于执行数据库操作
    // 构造器注入：Spring 创建这个 Bean 时自动把 ChatMessageMapper 传进来
    private final ChatMessageMapper chatMessageMapper;

    public DatabaseChatMemoryRepository(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 查询所有会话 ID
     * 用途：可以知道数据库里存了哪些对话
     */
    @Override
    public List<String> findConversationIds() {
        return chatMessageMapper.findAllConversationIds();
    }

    /**
     * 根据会话 ID 查询该会话的所有历史消息
     * 调用时机：每次用户发消息前，MessageWindowChatMemory 调用此方法读取历史，拼到请求里发给 AI
     *
     * 这里做了一次转换：数据库里存的是 ChatMessageEntity，
     * 但 Spring AI 需要的是 Message 对象，所以用 toMessage() 逐条转换
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        return chatMessageMapper.findByConversationId(conversationId)
                .stream()
                .map(this::toMessage)   // ChatMessageEntity → Message
                .collect(Collectors.toList());
    }

    /**
     * 保存某个会话的所有消息（先删后存）
     * 调用时机：每次 AI 回复后，MessageWindowChatMemory 调用此方法把最新的消息列表存入数据库
     *
     * 为什么先删后存，而不是只追加新消息？
     * 因为 MessageWindowChatMemory 会做窗口截断（只保留最近 N 条），
     * 截断后的完整列表需要整体替换数据库里的旧数据，所以先删再全量插入。
     */
    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        // 先删除该会话的所有旧消息
        chatMessageMapper.deleteByConversationId(conversationId);

        // 再把新的消息列表逐条插入
        for (Message message : messages) {
            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setConversationId(conversationId);
            entity.setRole(getRoleName(message));   // 判断是 user 还是 assistant
            entity.setContent(message.getText());   // 取消息文本
            chatMessageMapper.insert(entity);
        }
    }

    /**
     * 删除某个会话的所有消息
     * 用途：清空指定会话的历史记录
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        chatMessageMapper.deleteByConversationId(conversationId);
    }

    /**
     * 把 Spring AI 的 Message 对象转成数据库存储的角色名字符串
     * UserMessage      → "user"
     * AssistantMessage → "assistant"
     * 其他（SystemMessage 等）→ "system"
     */
    private String getRoleName(Message message) {
        if (message instanceof UserMessage) return "user";
        if (message instanceof AssistantMessage) return "assistant";
        return "system";
    }

    /**
     * 把数据库里的 ChatMessageEntity 转成 Spring AI 的 Message 对象
     * 为什么需要转换：Spring AI 的 ChatClient 只认识 Message 类型，不认识 ChatMessageEntity
     *
     * "assistant" → AssistantMessage（AI 的回复）
     * 其他        → UserMessage（用户的消息）
     */
    private Message toMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case "assistant" -> new AssistantMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }
}
