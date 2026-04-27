package com.wly.ai_agent_plus.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * DashScope HTTP 客户端配置
 * 解决 API 调用超时问题
 * 
 * 注意：Spring 6.1+ 推荐使用 JDK HttpClient 而不是 OkHttp
 */
@Configuration
public class DashScopeClientConfig {

    /**
     * 配置 JDK HttpClient，增加超时时间
     * RAG 场景下，由于需要检索向量数据库并生成较长回复，需要更长的超时时间
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))  // 连接超时：60秒
                .build();
    }

    /**
     * 自定义 RestClient，让 Spring AI 使用我们配置的 HttpClient
     * 这是关键：Spring AI Alibaba 使用 RestClient，需要通过 RestClientCustomizer 注入超时配置
     */
    @Bean
    public RestClientCustomizer restClientCustomizer(HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(600));  // 读取超时：600秒（10分钟）
        
        return restClientBuilder -> restClientBuilder
                .requestFactory(requestFactory);
    }
}
