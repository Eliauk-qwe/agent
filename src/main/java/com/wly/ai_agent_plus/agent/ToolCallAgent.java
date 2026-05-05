package com.wly.ai_agent_plus.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.wly.ai_agent_plus.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ToolCallAgent extends ReActAgent {
    private final ToolCallback[] tools;

    private ChatResponse toolCallChatResponse;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    private final List<String> processEvents = new ArrayList<>();

    public ToolCallAgent(ToolCallback[] tools) {
        this.tools = tools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .withToolCallbacks(Arrays.asList(tools))
                .build();
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        try {
            ChatResponse chatResponse = getChatClient().prompt(new Prompt(getMessageList(), chatOptions))
                    .system(getSystemPrompt())
                    .toolCallbacks(tools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            String result = assistantMessage.getText();

            log.info("{} 的思考: {}", getName(), result);
            log.info("{} 选择了 {} 个工具来使用", getName(), toolCallList.size());

            // 将AI的回复添加到消息列表中
            getMessageList().add(assistantMessage);

            if (toolCallList.isEmpty()) {
                if (StrUtil.isNotBlank(result)) {
                    processEvents.add("直接回答用户，无需调用工具。");
                }
                // 如果没有工具调用，说明AI已经直接回复了用户，任务完成
                setState(AgentState.FINISHED);
                return false;
            } else {
                String toolCallInfo = toolCallList.stream()
                        .map(toolCall -> String.format("工具名称: %s, 参数: %s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                processEvents.add("准备调用工具：\n" + toolCallInfo);
                return true;
            }
        } catch (Exception e) {
            log.error("{} 的思考过程遇到了问题: {}", getName(), e.getMessage(), e);
            getMessageList().add(new AssistantMessage("处理时遇到了错误: " + e.getMessage()));
            setState(AgentState.ERROR);
            return false;
        }
    }

    @Override
    public String act() {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        setMessageList(toolExecutionResult.conversationHistory());

        Message lastMessage = CollUtil.getLast(toolExecutionResult.conversationHistory());
        if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
            return "工具调用已完成，但未返回工具响应消息";
        }

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        String toolSummary = toolResponseMessage.getResponses().stream()
                .map(response -> {
                    String data = response.responseData();
                    if (data != null && data.length() > 500) {
                        data = data.substring(0, 500) + "...";
                    }
                    return "工具 " + response.name() + " 返回：" + data;
                })
                .collect(Collectors.joining("\n"));
        if (StrUtil.isNotBlank(toolSummary)) {
            processEvents.add(toolSummary);
        }

        return "[[NO_ANSWER]]工具调用完成，继续分析工具结果。";
    }

    @Override
    protected List<String> consumeProcessEvents() {
        List<String> events = new ArrayList<>(processEvents);
        processEvents.clear();
        return events;
    }
}
