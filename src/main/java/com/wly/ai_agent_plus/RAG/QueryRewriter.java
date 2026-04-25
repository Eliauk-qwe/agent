package com.wly.ai_agent_plus.RAG;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询重写组件
 *
 * 作用：在用户问题送去向量检索之前，先用 LLM 把问题改写成更适合检索的形式。
 *
 * 为什么需要查询重写：
 *   用户的原始问题往往是口语化的、模糊的，比如：
 *     "我男朋友最近不理我怎么办"
 *   这种问题直接去向量库检索，可能匹配不到最相关的文档。
 *   改写后变成：
 *     "恋爱关系中伴侣冷漠疏远的原因和应对方法"
 *   这样检索效果更好。
 *
 * 调用关系：
 *   LoveApp.chatwithrag()
 *     → QueryRewriter.rewrite()  ← 先改写问题
 *       → 直接调用 LLM 生成改写后的问题
 *     → QuestionAnswerAdvisor    ← 用改写后的问题检索向量库
 */
@Component
@Slf4j
public class QueryRewriter implements QueryTransformer {

    private final DashScopeChatModel chatModel;
    private final DashScopeChatOptions options;

    private static final String REWRITE_PROMPT_TEMPLATE = """
            你是一个查询重写专家。你的任务是将用户的口语化问题改写成更适合向量检索的形式。
            
            要求：
            1. 保持原问题的核心意图
            2. 使用更正式、更具体的表达
            3. 提取关键概念和主题
            4. 只返回改写后的问题，不要有任何解释
            
            用户问题：{query}
            
            改写后的问题：
            """;

    /**
     * 注入 DashScopeChatModel，用于驱动查询重写
     * 
     * 改进说明：
     *   不使用 RewriteQueryTransformer，而是直接调用 ChatModel，
     *   这样可以完全控制选项，确保 enableThinking=false 生效。
     */
    public QueryRewriter(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        // Qwen3 系列模型默认开启深度思考，非流式调用必须显式关闭，否则报 url error
        this.options = DashScopeChatOptions.builder()
                .enableThinking(false)
                .build();
    }

    /**
     * 对用户问题进行查询重写
     *
     * @param originalQuery 用户原始问题
     * @return 改写后的问题（更适合向量检索）
     */
    public String rewrite(String originalQuery) {
        log.debug("查询重写 - 原始: {}", originalQuery);

        try {
            // 构造重写提示词
            String promptText = REWRITE_PROMPT_TEMPLATE.replace("{query}", originalQuery);
            
            // 创建 Prompt，显式设置 enableThinking=false
            Prompt prompt = new Prompt(
                    List.of(new UserMessage(promptText)),
                    options
            );
            
            // 调用模型
            String rewrittenQuery = chatModel.call(prompt).getResult().getOutput().getText();
            
            log.debug("查询重写 - 改写后: {}", rewrittenQuery);
            return rewrittenQuery.trim();
        } catch (Exception e) {
            log.warn("查询重写失败，使用原始问题: {}", e.getMessage());
            return originalQuery;
        }
    }

    @Override
    public Query transform(Query query) {
        String rewrittenText = rewrite(query.text());
        return new Query(rewrittenText);
    }
}
