package com.wly.ai_agent_plus.advisor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class myadvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        logResponse(chatClientRequest, chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logRequest(chatClientRequest);

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(
                chatClientResponses, 
                response -> logResponse(chatClientRequest, response)
        );
    }

    protected void logRequest(ChatClientRequest request) {
        log.debug("request: {}", request.prompt().getUserMessage().getText());
    }

    protected void logResponse(ChatClientRequest request, ChatClientResponse chatClientResponse) {
        // 检查是否触发了违禁词
        Boolean bannedDetected = (Boolean) request.context().get(SafeGuardAdvisor.BANNED_FLAG_KEY);
        
        String responseText = chatClientResponse.chatResponse().getResult().getOutput().getText();
        
        if (Boolean.TRUE.equals(bannedDetected)) {
            // 违禁词场景，使用 info 级别确保一定输出
            log.info("抱歉，您的消息包含敏感词汇，无法为您提供回答。请调整您的问题后重试。");
        } else {
            // 正常场景，使用 debug 级别
            log.debug("response: {}", responseText);
        }
    }

    @Override
    public String getName() {
        return "myadvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }


}


