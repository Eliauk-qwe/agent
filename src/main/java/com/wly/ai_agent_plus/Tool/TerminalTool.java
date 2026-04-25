package com.wly.ai_agent_plus.Tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 终端操作工具
 * 
 * 允许 AI Agent 执行命令行操作
 * 
 * 功能：
 *   - 执行 Shell 命令
 *   - 支持工作目录切换
 *   - 支持超时控制
 *   - 捕获标准输出和错误输出
 *   - 安全限制（黑名单命令）
 * 
 * ⚠️ 安全警告：
 *   此工具允许执行系统命令，存在安全风险
 *   生产环境使用时必须：
 *   1. 严格限制可执行的命令
 *   2. 验证用户权限
 *   3. 记录所有操作日志
 *   4. 使用沙箱环境
 */
@Component
@Slf4j
public class TerminalTool implements Function<TerminalTool.Request, TerminalTool.Response> {

    // 默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT = 30;
    
    // 最大输出长度（字符）
    private static final int MAX_OUTPUT_LENGTH = 10000;
    
    // 危险命令黑名单
    private static final List<String> DANGEROUS_COMMANDS = List.of(
        "rm -rf /",
        "rm -rf /*",
        "mkfs",
        "dd if=/dev/zero",
        ":(){ :|:& };:",  // Fork bomb
        "chmod -R 777 /",
        "shutdown",
        "reboot",
        "halt",
        "poweroff"
    );

    /**
     * 执行终端命令
     * 
     * @param request 命令请求
     * @return 执行结果
     */
    @Tool(name = "executeTerminalCommand", description = "Execute terminal commands with optional working directory and timeout. Returns command output and exit code.")
    @Override
    public Response apply(@ToolParam(description = "Terminal command request with command string, optional working directory, and timeout") Request request) {
        log.info("执行终端命令: {}", request.command);
        
        Response response = new Response();
        response.command = request.command;
        response.workingDirectory = request.workingDirectory;
        
        // 安全检查
        if (!isSafeCommand(request.command)) {
            response.success = false;
            response.error = "拒绝执行：命令包含危险操作";
            log.warn("拒绝执行危险命令: {}", request.command);
            return response;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建进程
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // 根据操作系统选择 shell
            if (isWindows()) {
                processBuilder.command("cmd.exe", "/c", request.command);
            } else {
                processBuilder.command("sh", "-c", request.command);
            }
            
            // 设置工作目录
            if (request.workingDirectory != null && !request.workingDirectory.isEmpty()) {
                File workDir = new File(request.workingDirectory);
                if (workDir.exists() && workDir.isDirectory()) {
                    processBuilder.directory(workDir);
                } else {
                    response.success = false;
                    response.error = "工作目录不存在: " + request.workingDirectory;
                    return response;
                }
            }
            
            // 合并标准输出和错误输出
            processBuilder.redirectErrorStream(true);
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 设置超时
            int timeout = request.timeout > 0 ? request.timeout : DEFAULT_TIMEOUT;
            
            // 读取输出（使用单独的线程，避免阻塞）
            StringBuilder output = new StringBuilder();
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        
                        // 限制输出长度
                        if (output.length() > MAX_OUTPUT_LENGTH) {
                            output.append("\n[输出过长，已截断]");
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略读取异常（进程可能被强制终止）
                }
            });
            outputReader.start();
            
            // 等待进程完成（带超时）
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            
            if (!finished) {
                // 超时，强制终止进程
                process.destroyForcibly();
                // 等待输出读取线程结束
                outputReader.join(1000);
                
                response.success = false;
                response.error = "命令执行超时（" + timeout + "秒）";
                response.output = output.toString();
                
                long executionTime = System.currentTimeMillis() - startTime;
                response.executionTimeMs = executionTime;
                
                log.warn("命令执行超时: {}, 耗时: {}ms", request.command, executionTime);
                return response;
            }
            
            // 等待输出读取完成
            outputReader.join(1000);
            
            // 获取退出码
            response.exitCode = process.exitValue();
            response.success = (response.exitCode == 0);
            response.output = output.toString();
            
            long executionTime = System.currentTimeMillis() - startTime;
            response.executionTimeMs = executionTime;
            
            log.info("命令执行完成，退出码: {}, 耗时: {}ms", response.exitCode, executionTime);
            
        } catch (Exception e) {
            log.error("命令执行失败: {}", e.getMessage(), e);
            response.success = false;
            response.error = "执行失败: " + e.getMessage();
        }
        
        return response;
    }

    /**
     * 检查命令是否安全
     */
    private boolean isSafeCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();
        
        // 检查黑名单
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCommand.contains(dangerous.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 判断是否为 Windows 系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 命令请求
     */
    public static class Request {
        /**
         * 要执行的命令（必填）
         */
        public String command;
        
        /**
         * 工作目录（可选）
         */
        public String workingDirectory;
        
        /**
         * 超时时间（秒，可选，默认 30 秒）
         */
        public int timeout;

        public Request() {}

        public Request(String command) {
            this.command = command;
        }

        public Request(String command, String workingDirectory) {
            this.command = command;
            this.workingDirectory = workingDirectory;
        }

        public Request(String command, String workingDirectory, int timeout) {
            this.command = command;
            this.workingDirectory = workingDirectory;
            this.timeout = timeout;
        }
    }

    /**
     * 命令响应
     */
    public static class Response {
        /**
         * 执行的命令
         */
        public String command;
        
        /**
         * 工作目录
         */
        public String workingDirectory;
        
        /**
         * 是否成功
         */
        public boolean success;
        
        /**
         * 退出码
         */
        public int exitCode;
        
        /**
         * 标准输出
         */
        public String output;
        
        /**
         * 错误信息
         */
        public String error;
        
        /**
         * 执行时间（毫秒）
         */
        public long executionTimeMs;

        /**
         * 格式化输出
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("【命令】\n").append(command).append("\n\n");
            
            if (workingDirectory != null) {
                sb.append("【工作目录】\n").append(workingDirectory).append("\n\n");
            }
            
            sb.append("【状态】\n");
            sb.append(success ? "✅ 成功" : "❌ 失败").append("\n");
            sb.append("退出码: ").append(exitCode).append("\n");
            sb.append("耗时: ").append(executionTimeMs).append("ms\n\n");
            
            if (output != null && !output.isEmpty()) {
                sb.append("【输出】\n").append(output).append("\n");
            }
            
            if (error != null && !error.isEmpty()) {
                sb.append("【错误】\n").append(error).append("\n");
            }
            
            return sb.toString();
        }
    }

    /**
     * 常用命令快捷方法
     */
    public static class Commands {
        /**
         * 列出目录内容
         */
        public static Request listDirectory(String path) {
            String command = isWindowsOS() 
                ? "dir \"" + path + "\"" 
                : "ls -la \"" + path + "\"";
            return new Request(command);
        }

        /**
         * 查看文件内容
         */
        public static Request catFile(String filePath) {
            String command = isWindowsOS() 
                ? "type \"" + filePath + "\"" 
                : "cat \"" + filePath + "\"";
            return new Request(command);
        }

        /**
         * 查找文件
         */
        public static Request findFile(String directory, String pattern) {
            String command = isWindowsOS() 
                ? "dir /s /b \"" + directory + "\\" + pattern + "\"" 
                : "find \"" + directory + "\" -name \"" + pattern + "\"";
            return new Request(command);
        }

        /**
         * 搜索文件内容
         */
        public static Request grepContent(String directory, String pattern) {
            String command = isWindowsOS() 
                ? "findstr /s /i \"" + pattern + "\" \"" + directory + "\\*.*\"" 
                : "grep -r \"" + pattern + "\" \"" + directory + "\"";
            return new Request(command);
        }

        /**
         * 获取当前目录
         */
        public static Request getCurrentDirectory() {
            String command = isWindowsOS() ? "cd" : "pwd";
            return new Request(command);
        }

        /**
         * 查看系统信息
         */
        public static Request getSystemInfo() {
            String command = isWindowsOS() ? "systeminfo" : "uname -a";
            return new Request(command);
        }

        /**
         * 查看磁盘使用情况
         */
        public static Request getDiskUsage() {
            String command = isWindowsOS() ? "wmic logicaldisk get size,freespace,caption" : "df -h";
            return new Request(command);
        }

        /**
         * 查看进程列表
         */
        public static Request getProcessList() {
            String command = isWindowsOS() ? "tasklist" : "ps aux";
            return new Request(command);
        }

        /**
         * 执行 Maven 命令
         */
        public static Request maven(String goals, String workingDirectory) {
            String command = isWindowsOS() 
                ? "mvn.cmd " + goals 
                : "mvn " + goals;
            return new Request(command, workingDirectory);
        }

        /**
         * 执行 Git 命令
         */
        public static Request git(String gitCommand, String workingDirectory) {
            return new Request("git " + gitCommand, workingDirectory);
        }

        private static boolean isWindowsOS() {
            return System.getProperty("os.name").toLowerCase().contains("win");
        }
    }
}
