package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 终端工具测试
 */
@SpringBootTest
class TerminalToolTest {

    @Autowired
    private TerminalTool terminalTool;

    @Test
    void testBasicCommand() {
        System.out.println("\n========== 测试 1: 基础命令 ==========\n");
        
        // 执行简单命令
        TerminalTool.Request request = new TerminalTool.Request("echo Hello World");
        TerminalTool.Response response = terminalTool.apply(request);
        
        // 验证
        assertTrue(response.success, "命令应该成功执行");
        assertEquals(0, response.exitCode, "退出码应该为 0");
        assertTrue(response.output.contains("Hello World"), "输出应该包含 Hello World");
        
        System.out.println(response.toFormattedString());
    }

    @Test
    void testListDirectory() {
        System.out.println("\n========== 测试 2: 列出目录 ==========\n");
        
        // 列出当前目录
        TerminalTool.Request request = TerminalTool.Commands.listDirectory(".");
        TerminalTool.Response response = terminalTool.apply(request);
        
        assertTrue(response.success);
        assertNotNull(response.output);
        assertFalse(response.output.isEmpty());
        
        System.out.println("目录内容:");
        System.out.println(response.output);
    }

    @Test
    void testGetCurrentDirectory() {
        System.out.println("\n========== 测试 3: 获取当前目录 ==========\n");
        
        TerminalTool.Request request = TerminalTool.Commands.getCurrentDirectory();
        TerminalTool.Response response = terminalTool.apply(request);
        
        assertTrue(response.success);
        System.out.println("当前目录: " + response.output.trim());
    }

    @Test
    void testWorkingDirectory() {
        System.out.println("\n========== 测试 4: 指定工作目录 ==========\n");
        
        // 在 src 目录下执行命令
        TerminalTool.Request request = new TerminalTool.Request(
            "ls -la",  // Linux/Mac
            "src"
        );
        
        TerminalTool.Response response = terminalTool.apply(request);
        
        if (response.success) {
            System.out.println("src 目录内容:");
            System.out.println(response.output);
        } else {
            System.out.println("命令执行失败（可能是 Windows 系统）");
        }
    }

    @Test
    void testFindFile() {
        System.out.println("\n========== 测试 5: 查找文件 ==========\n");
        
        // 查找 pom.xml
        TerminalTool.Request request = TerminalTool.Commands.findFile(".", "pom.xml");
        TerminalTool.Response response = terminalTool.apply(request);
        
        if (response.success) {
            System.out.println("找到的文件:");
            System.out.println(response.output);
        }
    }

    @Test
    void testGitCommand() {
        System.out.println("\n========== 测试 6: Git 命令 ==========\n");
        
        // 查看 Git 状态
        TerminalTool.Request request = TerminalTool.Commands.git("status", ".");
        TerminalTool.Response response = terminalTool.apply(request);
        
        if (response.success) {
            System.out.println("Git 状态:");
            System.out.println(response.output);
        } else {
            System.out.println("Git 命令失败（可能不是 Git 仓库）");
        }
    }

    @Test
    void testMavenCommand() {
        System.out.println("\n========== 测试 7: Maven 命令 ==========\n");
        
        // 执行 Maven 命令（只验证，不实际编译）
        TerminalTool.Request request = TerminalTool.Commands.maven("--version", ".");
        TerminalTool.Response response = terminalTool.apply(request);
        
        if (response.success) {
            System.out.println("Maven 版本:");
            System.out.println(response.output);
        } else {
            System.out.println("Maven 命令失败");
        }
    }

    @Test
    void testTimeout() {
        System.out.println("\n========== 测试 8: 超时控制 ==========\n");
        
        // 执行一个会超时的命令
        // Linux/Mac: sleep 10
        // Windows: timeout /t 10
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String command = isWindows ? "timeout /t 10" : "sleep 10";
        
        TerminalTool.Request request = new TerminalTool.Request(command);
        request.timeout = 2;  // 2 秒超时
        
        long startTime = System.currentTimeMillis();
        TerminalTool.Response response = terminalTool.apply(request);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("执行时间: " + duration + "ms");
        System.out.println("成功状态: " + response.success);
        System.out.println("错误信息: " + response.error);
        
        // 验证：应该在 2-3 秒内超时（不会等到 10 秒）
        assertTrue(duration < 4000, "应该在 4 秒内超时，实际: " + duration + "ms");
        
        if (!response.success && response.error != null && response.error.contains("超时")) {
            System.out.println("✅ 超时控制正常工作");
        } else {
            System.out.println("⚠️ 超时可能未生效（某些系统上 sleep/timeout 命令行为不同）");
        }
    }

    @Test
    void testDangerousCommand() {
        System.out.println("\n========== 测试 9: 危险命令拦截 ==========\n");
        
        // 尝试执行危险命令
        TerminalTool.Request request = new TerminalTool.Request("rm -rf /");
        TerminalTool.Response response = terminalTool.apply(request);
        
        assertFalse(response.success, "危险命令应该被拒绝");
        assertTrue(response.error.contains("危险"), "错误信息应该包含'危险'");
        
        System.out.println("✅ 危险命令已被拦截");
        System.out.println(response.error);
    }

    @Test
    void testInvalidDirectory() {
        System.out.println("\n========== 测试 10: 无效工作目录 ==========\n");
        
        TerminalTool.Request request = new TerminalTool.Request(
            "ls",
            "/this/directory/does/not/exist"
        );
        
        TerminalTool.Response response = terminalTool.apply(request);
        
        assertFalse(response.success, "无效目录应该失败");
        assertTrue(response.error.contains("不存在"), "错误信息应该说明目录不存在");
        
        System.out.println(response.error);
    }

    @Test
    void testSystemInfo() {
        System.out.println("\n========== 测试 11: 系统信息 ==========\n");
        
        TerminalTool.Request request = TerminalTool.Commands.getSystemInfo();
        TerminalTool.Response response = terminalTool.apply(request);
        
        if (response.success) {
            System.out.println("系统信息:");
            // 只显示前 500 个字符
            String output = response.output;
            System.out.println(output.length() > 500 
                ? output.substring(0, 500) + "..." 
                : output);
        }
    }

    @Test
    void testMultipleCommands() {
        System.out.println("\n========== 测试 12: 多个命令 ==========\n");
        
        String[] commands = {
            "echo 'Test 1'",
            "echo 'Test 2'",
            "echo 'Test 3'"
        };
        
        for (String cmd : commands) {
            TerminalTool.Response response = terminalTool.apply(
                new TerminalTool.Request(cmd)
            );
            System.out.println(cmd + " -> " + response.output.trim());
        }
    }
}
