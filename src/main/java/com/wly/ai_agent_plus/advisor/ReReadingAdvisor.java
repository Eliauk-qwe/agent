package com.wly.ai_agent_plus.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-Reading（RE²）Advisor
 *
 * 基于论文《Re-Reading Improves Reasoning in Large Language Models》的提示技术。
 * 核心思想：在发送给模型之前，将用户问题重复两遍，让模型更仔细地理解问题，
 * 从而提升复杂推理任务的准确率。
 *
 * 例如用户输入"你是谁？"，实际发给模型的内容会变成：
 *   你是谁？
 *   Read the question again: 你是谁？
 */
public class ReReadingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 获取所有消息
        List<Message> messages = request.prompt().getInstructions();
        
        // 找到最后一条用户消息
        String lastUserText = null;
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                lastUserText = msg.getText();
                lastUserIndex = i;
                break;
            }
        }

        if (lastUserText == null) {
            return chain.nextCall(request);
        }

        // 构造重复的消息
        String enhancedText = lastUserText + "\nRead the question again: " + lastUserText;

        // 创建新的消息列表
        List<Message> newMessages = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (i == lastUserIndex) {
                // 替换最后一条用户消息
                newMessages.add(new UserMessage(enhancedText));
            } else {
                newMessages.add(messages.get(i));
            }
        }

        // 创建新的 Prompt
        Prompt newPrompt = new Prompt(newMessages, request.prompt().getOptions());

        // 修改请求
        ChatClientRequest modifiedRequest = request.mutate()
                .prompt(newPrompt)
                .build();

        // 继续调用链
        return chain.nextCall(modifiedRequest);
    }

    @Override
    public String getName() {
        return "ReReadingAdvisor";
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
