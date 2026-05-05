package com.wly.ai_agent_plus.controller;

import com.wly.ai_agent_plus.agent.MyManus;
import com.wly.ai_agent_plus.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 LoveApp。
     */
    @GetMapping("/love/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.chat(message, chatId);
    }

    /**
     * 流式调用 LoveApp。
     * 
     * 确保只返回最终的文本内容，过滤掉任何元数据或工具调用信息。
     */
    @GetMapping(value = "/love/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSse(String message, String chatId) {
        return loveApp.chatStreamWithRag(message, chatId)
                .doOnError(error -> {
                    // 记录任何流式处理错误
                    error.printStackTrace();
                });
    }

    /**
     * 流式调用 MyManus 超级智能体。
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(String message) {
        MyManus myManus = new MyManus(allTools, dashscopeChatModel);
        return myManus.runStream(message);
    }
}
