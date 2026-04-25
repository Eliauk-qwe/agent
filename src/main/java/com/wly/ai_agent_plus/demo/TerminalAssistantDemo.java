package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.TerminalTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 终端助手示例
 * 
 * 演示如何让 AI 使用终端工具完成任务
 */
@Component
@Slf4j
public class TerminalAssistantDemo {

    @Autowired
    private TerminalTool terminalTool;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 示例 1: 项目信息查询
     * 
     * AI 通过执行命令了解项目结构
     */
    public String analyzeProject(String projectPath) {
        log.info("=== 分析项目: {} ===", projectPath);
        
        StringBuilder projectInfo = new StringBuilder();
        
        // 1. 查看项目根目录
        TerminalTool.Response listResponse = terminalTool.apply(
            TerminalTool.Commands.listDirectory(projectPath)
        );
        
        if (listResponse.success) {
            projectInfo.append("【项目根目录】\n").append(listResponse.output).append("\n\n");
        }
        
        // 2. 查看 pom.xml（如果是 Maven 项目）
        TerminalTool.Response pomResponse = terminalTool.apply(
            TerminalTool.Commands.catFile(projectPath + "/pom.xml")
        );
        
        if (pomResponse.success) {
            projectInfo.append("【pom.xml 内容】\n");
            // 只取前 1000 个字符
            String pomContent = pomResponse.output;
            projectInfo.append(pomContent.length() > 1000 
                ? pomContent.substring(0, 1000) + "..." 
                : pomContent);
            projectInfo.append("\n\n");
        }
        
        // 3. 查看 Git 状态
        TerminalTool.Response gitResponse = terminalTool.apply(
            TerminalTool.Commands.git("status", projectPath)
        );
        
        if (gitResponse.success) {
            projectInfo.append("【Git 状态】\n").append(gitResponse.output).append("\n\n");
        }
        
        // 4. 让 AI 分析
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                请分析以下项目信息：
                
                %s
                
                请回答：
                1. 这是什么类型的项目？
                2. 使用了哪些主要技术？
                3. 项目的基本结构是什么？
                4. 有什么建议？
                """, projectInfo.toString());
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 2: 代码搜索
     * 
     * 在项目中搜索特定代码
     */
    public String searchCode(String projectPath, String searchPattern) {
        log.info("=== 搜索代码: {} ===", searchPattern);
        
        // 搜索代码
        TerminalTool.Response searchResponse = terminalTool.apply(
            TerminalTool.Commands.grepContent(projectPath, searchPattern)
        );
        
        if (!searchResponse.success) {
            return "搜索失败: " + searchResponse.error;
        }
        
        // AI 分析搜索结果
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                在项目中搜索"%s"，找到以下结果：
                
                %s
                
                请总结：
                1. 找到了多少处匹配？
                2. 主要在哪些文件中？
                3. 这些代码的用途是什么？
                """, searchPattern, searchResponse.output);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 3: 执行构建任务
     * 
     * 执行 Maven 构建并分析结果
     */
    public String buildProject(String projectPath) {
        log.info("=== 构建项目 ===");
        
        // 执行 Maven clean compile
        TerminalTool.Request buildRequest = TerminalTool.Commands.maven(
            "clean compile -DskipTests",
            projectPath
        );
        buildRequest.timeout = 300;  // 5 分钟超时
        
        TerminalTool.Response buildResponse = terminalTool.apply(buildRequest);
        
        // AI 分析构建结果
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                Maven 构建结果：
                
                状态: %s
                退出码: %d
                耗时: %dms
                
                输出:
                %s
                
                请分析：
                1. 构建是否成功？
                2. 如果失败，原因是什么？
                3. 有什么警告或建议？
                """, 
                buildResponse.success ? "成功" : "失败",
                buildResponse.exitCode,
                buildResponse.executionTimeMs,
                buildResponse.output);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 4: 系统诊断
     * 
     * 收集系统信息并诊断问题
     */
    public String diagnoseSystem() {
        log.info("=== 系统诊断 ===");
        
        StringBuilder systemInfo = new StringBuilder();
        
        // 1. 系统信息
        TerminalTool.Response sysInfoResponse = terminalTool.apply(
            TerminalTool.Commands.getSystemInfo()
        );
        if (sysInfoResponse.success) {
            systemInfo.append("【系统信息】\n");
            String output = sysInfoResponse.output;
            systemInfo.append(output.length() > 500 ? output.substring(0, 500) + "..." : output);
            systemInfo.append("\n\n");
        }
        
        // 2. 磁盘使用情况
        TerminalTool.Response diskResponse = terminalTool.apply(
            TerminalTool.Commands.getDiskUsage()
        );
        if (diskResponse.success) {
            systemInfo.append("【磁盘使用】\n").append(diskResponse.output).append("\n\n");
        }
        
        // 3. Java 版本
        TerminalTool.Response javaResponse = terminalTool.apply(
            new TerminalTool.Request("java -version")
        );
        if (javaResponse.success) {
            systemInfo.append("【Java 版本】\n").append(javaResponse.output).append("\n\n");
        }
        
        // 4. Maven 版本
        TerminalTool.Response mavenResponse = terminalTool.apply(
            TerminalTool.Commands.maven("--version", ".")
        );
        if (mavenResponse.success) {
            systemInfo.append("【Maven 版本】\n").append(mavenResponse.output).append("\n\n");
        }
        
        // AI 诊断
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                系统信息：
                
                %s
                
                请诊断：
                1. 系统配置是否正常？
                2. 是否有潜在问题？
                3. 有什么优化建议？
                """, systemInfo.toString());
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 5: Git 操作助手
     * 
     * 帮助用户执行 Git 操作
     */
    public String gitAssistant(String projectPath, String operation) {
        log.info("=== Git 助手: {} ===", operation);
        
        TerminalTool.Response response = null;
        
        switch (operation.toLowerCase()) {
            case "status":
                response = terminalTool.apply(
                    TerminalTool.Commands.git("status", projectPath)
                );
                break;
                
            case "log":
                response = terminalTool.apply(
                    TerminalTool.Commands.git("log --oneline -10", projectPath)
                );
                break;
                
            case "branch":
                response = terminalTool.apply(
                    TerminalTool.Commands.git("branch -a", projectPath)
                );
                break;
                
            case "diff":
                response = terminalTool.apply(
                    TerminalTool.Commands.git("diff --stat", projectPath)
                );
                break;
                
            default:
                return "不支持的操作: " + operation;
        }
        
        if (!response.success) {
            return "Git 操作失败: " + response.error;
        }
        
        // AI 解释结果
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                Git %s 的结果：
                
                %s
                
                请用简单的语言解释这个结果。
                """, operation, response.output);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 6: 文件查找助手
     * 
     * 帮助用户查找文件
     */
    public String findFiles(String directory, String pattern, String purpose) {
        log.info("=== 查找文件: {} ===", pattern);
        
        // 查找文件
        TerminalTool.Response findResponse = terminalTool.apply(
            TerminalTool.Commands.findFile(directory, pattern)
        );
        
        if (!findResponse.success) {
            return "查找失败: " + findResponse.error;
        }
        
        // AI 分析结果
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = String.format("""
                用户想要: %s
                
                在 %s 目录下查找 "%s"，找到以下文件：
                
                %s
                
                请：
                1. 总结找到了哪些文件
                2. 根据用户的目的，推荐最相关的文件
                3. 给出下一步建议
                """, purpose, directory, pattern, findResponse.output);
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 示例 7: 智能命令执行
     * 
     * AI 根据用户意图自动选择和执行命令
     */
    public String smartExecute(String userIntent, String projectPath) {
        log.info("=== 智能执行: {} ===", userIntent);
        
        // 第一步：让 AI 决定执行什么命令
        ChatClient chatClient = chatClientBuilder.build();
        
        String planPrompt = String.format("""
                用户意图：%s
                项目路径：%s
                
                请决定需要执行什么命令来完成用户的意图。
                只返回命令本身，不要有任何解释。
                如果需要多个命令，用换行分隔。
                
                示例：
                - 如果用户想查看项目结构，返回：ls -la
                - 如果用户想查看 Git 状态，返回：git status
                - 如果用户想编译项目，返回：mvn clean compile
                """, userIntent, projectPath);
        
        String commands = chatClient.prompt()
                .user(planPrompt)
                .call()
                .content();
        
        log.info("AI 决定执行命令: {}", commands);
        
        // 第二步：执行命令
        StringBuilder results = new StringBuilder();
        String[] commandList = commands.split("\n");
        
        for (String cmd : commandList) {
            cmd = cmd.trim();
            if (cmd.isEmpty()) continue;
            
            TerminalTool.Response response = terminalTool.apply(
                new TerminalTool.Request(cmd, projectPath)
            );
            
            results.append("【命令】").append(cmd).append("\n");
            results.append("【结果】").append(response.success ? "成功" : "失败").append("\n");
            results.append(response.output).append("\n\n");
        }
        
        // 第三步：AI 总结结果
        String summaryPrompt = String.format("""
                用户意图：%s
                
                执行结果：
                %s
                
                请用简单的语言总结执行结果，回答用户的问题。
                """, userIntent, results.toString());
        
        return chatClient.prompt()
                .user(summaryPrompt)
                .call()
                .content();
    }
}
