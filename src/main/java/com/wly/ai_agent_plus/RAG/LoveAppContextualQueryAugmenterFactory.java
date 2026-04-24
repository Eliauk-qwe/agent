package com.wly.ai_agent_plus.RAG;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.core.io.Resource;

/**
 * ContextualQueryAugmenter 工厂类
 *
 * 作用：当向量检索结果为空时（即知识库里没有相关内容），
 *       拦截请求，返回固定的提示语，而不是让 LLM 自由发挥乱答。
 *
 * 为什么需要这个：
 *   RAG 的核心是"基于知识库回答"。如果检索不到相关文档，
 *   LLM 可能会凭空编造答案，或者回答与恋爱无关的问题。
 *   通过 allowEmptyContext(false)，强制要求必须有检索结果才能回答，
 *   否则返回固定的拒绝语，引导用户提问恋爱相关问题。
 *
 * 调用关系：
 *   LoveApp.chatwithrag()
 *     → QueryRewriter.rewrite()              ← 改写问题
 *     → QuestionAnswerAdvisor                ← 检索向量库 + 注入文档
 *         内部使用 ContextualQueryAugmenter  ← 本类创建的实例处理空结果
 *         → 有结果：把文档内容注入到 prompt，让 LLM 基于知识库回答
 *         → 无结果：返回固定提示语，不调用 LLM
 */
public class LoveAppContextualQueryAugmenterFactory {

    /**
     * 创建 ContextualQueryAugmenter 实例（使用自定义 RAG 提示词模板）
     *
     * @param ragPromptResource rag-prompt-template.st，定义如何把文档拼入 prompt
     *                          模板必须包含 {query} 和 {context} 两个占位符
     */
    public static ContextualQueryAugmenter createInstance(Resource ragPromptResource) {
        // 有检索结果时使用的模板（自定义，中文，有角色要求）
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptResource);

        // 检索结果为空时的提示语
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，有问题可以联系编程导航客服 https://codefather.cn
                """);

        return ContextualQueryAugmenter.builder()
                .promptTemplate(promptTemplate)
                // false：检索结果为空时，不允许 LLM 自由回答，直接用 emptyContextPromptTemplate
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }

    /**
     * 创建 ContextualQueryAugmenter 实例（使用默认英文模板）
     */
    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，有问题可以联系编程导航客服 https://codefather.cn
                """);

        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
