package com.wly.ai_agent_plus.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

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
public class ReReadingAdvisor implements BaseAdvisor {

    // 默认模板：将用户问题重复两遍
    private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
            """;

    // 实际使用的模板，支持自定义
    private final String re2AdviseTemplate;

    /**
     * 使用默认模板构造
     */
    public ReReadingAdvisor() {
        this(DEFAULT_RE2_ADVISE_TEMPLATE);
    }

    /**
     * 使用自定义模板构造
     * @param re2AdviseTemplate 自定义提示模板，需包含 {re2_input_query} 占位符
     */
    public ReReadingAdvisor(String re2AdviseTemplate) {
        this.re2AdviseTemplate = re2AdviseTemplate;
    }

    /**
     * 请求发送给模型之前执行
     * 将用户原始问题填入模板，生成重复问题的增强文本，替换原始用户消息
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 用模板渲染增强后的用户提示词，将 {re2_input_query} 替换为实际用户输入
        String augmentedUserText = PromptTemplate.builder()
                .template(this.re2AdviseTemplate)
                .variables(Map.of("re2_input_query", chatClientRequest.prompt().getUserMessage().getText()))
                .build()
                .render();

        // 用增强后的文本替换原始用户消息，其余请求内容不变
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .build();
    }

    /**
     * 模型响应返回后执行
     * 此 Advisor 不需要处理响应，直接透传
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {

        return chatClientResponse;
    }

    /**
     * Advisor 执行顺序，数字越小越先执行
     * 设为 -1，在记忆 Advisor（通常为 0）之前执行
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
