package com.wly.ai_agent_plus.mapper;

import com.wly.ai_agent_plus.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Select("SELECT DISTINCT conversation_id FROM chat_message")
    List<String> findAllConversationIds();

    @Insert("INSERT INTO chat_message(conversation_id, role, content) VALUES(#{conversationId}, #{role}, #{content})")
    void insert(ChatMessageEntity entity);

    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessageEntity> findByConversationId(String conversationId);

    @Delete("DELETE FROM chat_message WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(String conversationId);
}
