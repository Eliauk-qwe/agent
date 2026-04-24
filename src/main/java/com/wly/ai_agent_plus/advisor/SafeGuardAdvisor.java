package com.wly.ai_agent_plus.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Set;

/**
 * 违禁词过滤 Advisor
 * 检测用户消息中的违禁词，如果包含则在 context 中标记
 */
public class SafeGuardAdvisor implements CallAdvisor {

    // 违禁词列表
    private static final Set<String> BANNED_WORDS = Set.of(
            "暴力", "色情", "赌博", "毒品"
    );

    // 在 context 中标记是否触发违禁词的 key
    public static final String BANNED_FLAG_KEY = "bannedWordDetected";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 检查用户消息是否包含违禁词
        List<Message> messages = request.prompt().getInstructions();
        for (Message message : messages) {
            if (message instanceof UserMessage) {
                String text = message.getText();
                for (String word : BANNED_WORDS) {
                    if (text.contains(word)) {
                        // 检测到违禁词，在 context 中设置标记
                        request.context().put(BANNED_FLAG_KEY, true);
                        break;
                    }
                }
            }
        }

        // 继续传递给下一个 Advisor 或模型
        return chain.nextCall(request);
    }

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
