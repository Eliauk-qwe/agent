package com.wly.ai_agent_plus.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Set;

/**
 * 违禁词过滤 Advisor
 * 检测用户消息中的违禁词，如果包含则直接返回拒绝消息，不调用模型
 */
public class SafeGuardAdvisor implements CallAdvisor {

    // 违禁词列表
    private static final Set<String> BANNED_WORDS = Set.of(
            "暴力", "色情", "赌博", "毒品"
    );

    // 违禁词提示消息
    private static final String BANNED_MESSAGE = "抱歉，您的消息包含敏感词汇，无法为您提供回答。请调整您的问题后重试。";

    // 在 context 中标记是否触发违禁词的 key
    public static final String BANNED_FLAG_KEY = "bannedWordDetected";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 检查用户消息是否包含违禁词
        String userMessage = request.prompt().getUserMessage().getText();
        for (String word : BANNED_WORDS) {
            if (userMessage.contains(word)) {
                // 检测到违禁词，在 context 中设置标记
                request.context().put(BANNED_FLAG_KEY, true);
                
            }
        }

        // 没有违禁词，继续传递给下一个 Advisor 或模型
        return chain.nextCall(request);
    }

    /**
     * 构造拒绝响应
     */
    // private ChatClientResponse createRejectionResponse(ChatClientRequest request) {
    //     AssistantMessage assistantMessage = new AssistantMessage(BANNED_MESSAGE);
    //     Generation generation = new Generation(assistantMessage);
    //     ChatResponse chatResponse = new ChatResponse(List.of(generation));
        
    //     return ChatClientResponse.builder()
    //             .chatResponse(chatResponse)
    //             .build();
    // }

    @Override
    public String getName() {
        return "SafeGuardAdvisor";
    }

    @Override
    public int getOrder() {
        // 最高优先级，第一个执行
        return Integer.MIN_VALUE;
    }
}
