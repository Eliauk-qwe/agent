package com.wly.ai_agent_plus.RAG;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

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
 *       → RewriteQueryTransformer（Spring AI 内置）
 *         → 调用 LLM 生成改写后的问题
 *     → QuestionAnswerAdvisor    ← 用改写后的问题检索向量库
 */
@Component
@Slf4j
public class QueryRewriter {

    // Spring AI 内置的查询重写转换器
    private final QueryTransformer queryTransformer;

    /**
     * 注入 ChatClient.Builder（通义千问），用于驱动查询重写
     * 
     * 改进说明：
     *   直接注入 ChatClient.Builder 而不是 ChatModel，
     *   这样可以继承 application.yml 中配置的超时时间等参数，
     *   避免查询重写步骤因使用默认超时配置而超时。
     */
    public QueryRewriter(ChatClient.Builder chatClientBuilder) {
        // 创建查询重写转换器
        // 默认 prompt：将用户问题改写为更适合向量检索的形式
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
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

        Query query = new Query(originalQuery);
        Query transformedQuery = queryTransformer.transform(query);
        String rewrittenQuery = transformedQuery.text();

        log.debug("查询重写 - 改写后: {}", rewrittenQuery);
        return rewrittenQuery;
    }
}
