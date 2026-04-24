package com.wly.ai_agent_plus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching  // 启用 Spring Cache 支持
public class AiAgentPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentPlusApplication.class, args);
    }

}



