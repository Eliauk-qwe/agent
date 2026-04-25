package com.wly.ai_agent_plus.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wly.ai_agent_plus.Tool.*;

/**
 * 工具注册配置
 * 将各种工具注册为可供 AI Agent 调用的工具
 */
@Configuration
public class ToolRegistration {

    @Autowired
    private FileOperation fileOperation;

    @Autowired
    private FileDownloadTool fileDownloadTool;

    @Autowired
    private WebScraperTool webScraperTool;

    @Autowired
    private TerminalTool terminalTool;

    @Autowired
    private PdfGeneratorTool pdfGeneratorTool;

    /**
     * 注册所有工具
     * Spring AI 会自动识别这些工具并在需要时调用
     */
    @Bean
    public Object[] allTools() {
        return new Object[] {
            fileOperation,
            webScraperTool,
            fileDownloadTool,
            terminalTool,
            pdfGeneratorTool
        };
    }
}
