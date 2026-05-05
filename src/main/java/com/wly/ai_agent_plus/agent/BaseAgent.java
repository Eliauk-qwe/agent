package com.wly.ai_agent_plus.agent;

import cn.hutool.core.util.StrUtil;
import com.wly.ai_agent_plus.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Data
public abstract class BaseAgent {
    private String name;

    private String systemPrompt;
    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int maxSteps = 5;  // 减少默认最大步数从10到5
    private int currentStep = 0;

    private ChatClient chatClient;

    private List<Message> messageList = new ArrayList<>();

    public String run(String userPrompt) {
        if (state != AgentState.IDLE) {
            throw new IllegalStateException("Cannot run agent from non-idle state: " + state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new IllegalArgumentException("User prompt cannot be blank");
        }

        state = AgentState.RUNNING;
        currentStep = 0;
        messageList.add(new UserMessage(userPrompt));

        StringBuilder finalResponse = new StringBuilder();
        List<String> toolResults = new ArrayList<>();

        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                currentStep = i + 1;
                log.info("Executing step: {}", currentStep);

                String result = step();
                log.info("step {}: {}", currentStep, result);

                // 判断是否是工具调用结果
                if (result.startsWith("工具 ") && result.contains(" 返回的结果: ")) {
                    // 这是工具调用结果，收集起来
                    toolResults.add(result);
                } else if (!result.equals("完成思考，无需行动") && !result.startsWith("[[NO_ANSWER]]")) {
                    // 这是AI的直接回复或最终回复
                    finalResponse.append(result);
                    if (state == AgentState.FINISHED) {
                        break;
                    }
                }
            }

            if (state != AgentState.FINISHED && currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                if (finalResponse.length() == 0) {
                    finalResponse.append("任务执行完成，但达到了最大步骤限制。");
                }
            }

            // 如果只有工具结果没有最终回复，返回工具结果
            if (finalResponse.length() == 0 && !toolResults.isEmpty()) {
                return String.join("\n", toolResults);
            }

            return finalResponse.toString().trim();
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("run agent error", e);
            return "执行错误: " + e.getMessage();
        } finally {
            clean();
        }
    }

    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (state != AgentState.IDLE) {
                    sseEmitter.send("错误: 无法从当前状态运行智能体: " + state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误: 用户提示词不能为空");
                    sseEmitter.complete();
                    return;
                }

                state = AgentState.RUNNING;
                currentStep = 0;
                messageList.add(new UserMessage(userPrompt));

                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    currentStep = i + 1;
                    log.info("Executing step {}/{}", currentStep, maxSteps);

                    String stepResult = step();
                    for (String processEvent : consumeProcessEvents()) {
                        sseEmitter.send("[[PROCESS]]Step " + currentStep + ": " + processEvent);
                    }
                    if (!stepResult.startsWith("[[NO_ANSWER]]")) {
                        sseEmitter.send("[[ANSWER]]Step " + currentStep + ": " + stepResult);
                    }
                }

                if (state != AgentState.FINISHED && currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    sseEmitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                }

                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("run stream agent error", e);
                try {
                    sseEmitter.send("执行错误: " + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                clean();
            }
        });

        sseEmitter.onTimeout(() -> {
            state = AgentState.ERROR;
            clean();
            log.warn("SSE connection timeout");
        });
        sseEmitter.onCompletion(() -> {
            if (state == AgentState.RUNNING) {
                state = AgentState.FINISHED;
            }
            clean();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    public abstract String step();

    protected List<String> consumeProcessEvents() {
        return Collections.emptyList();
    }

    public void clean() {
        state = AgentState.IDLE;
        currentStep = 0;
        messageList.clear();
    }
}
