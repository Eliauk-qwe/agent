package com.wly.ai_agent_plus.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring AI 调用大模型示例
 * CommandLineRunner：Spring Boot 启动完成后自动执行 run 方法
 */
//@Component 告诉 Spring：把这个类交给我管理，也就是让 Spring 创建这个类的实例（Bean）并放入容器里。
//@Component  // 注释掉，避免启动时自动执行干扰测试
public class SpringAI implements CommandLineRunner {

    // 注入 Spring AI 提供的聊天模型（具体实现由配置决定，如 OpenAI、通义千问等）
    //从 Spring 容器里取出对应的 Bean，赋值给这个字段。
    @Resource
    private ChatModel chatModel;

    @Override
    public void run(String... args) throws Exception {
        // 构建 Prompt 并调用模型，链式获取响应文本
        String text = chatModel.call(new Prompt("你好，我是你的顾客"))
                .getResult()   // 获取第一个生成结果
                .getOutput()   // 获取 AssistantMessage
                .getText();    // 提取文本内容

        System.out.println("SpringAI 响应：");
        System.out.println(text);
    }
}
