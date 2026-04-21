package com.wly.ai_agent_plus.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageEntity {
    private Long id;
    private String conversationId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
