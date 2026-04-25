package com.wly.ai_agent_plus.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 终端助手示例测试
 */
@SpringBootTest
class TerminalAssistantDemoTest {

    @Autowired
    private TerminalAssistantDemo demo;

    @Test
    void testAnalyzeProject() {
        System.out.println("\n========== 示例 1: 分析项目 ==========\n");
        
        String analysis = demo.analyzeProject(".");
        
        System.out.println("【AI 分析】");
        System.out.println(analysis);
    }

    @Test
    void testSearchCode() {
        System.out.println("\n========== 示例 2: 搜索代码 ==========\n");
        
        String result = demo.searchCode("src", "@Component");
        
        System.out.println("【搜索结果分析】");
        System.out.println(result);
    }

    @Test
    void testBuildProject() {
        System.out.println("\n========== 示例 3: 构建项目 ==========\n");
        
        // 注意：这会实际执行 Maven 构建，可能需要较长时间
        String result = demo.buildProject(".");
        
        System.out.println("【构建分析】");
        System.out.println(result);
    }

    @Test
    void testDiagnoseSystem() {
        System.out.println("\n========== 示例 4: 系统诊断 ==========\n");
        
        String diagnosis = demo.diagnoseSystem();
        
        System.out.println("【系统诊断】");
        System.out.println(diagnosis);
    }

    @Test
    void testGitAssistant() {
        System.out.println("\n========== 示例 5: Git 助手 ==========\n");
        
        String[] operations = {"status", "log", "branch"};
        
        for (String op : operations) {
            System.out.println("\n--- Git " + op + " ---");
            String result = demo.gitAssistant(".", op);
            System.out.println(result);
        }
    }

    @Test
    void testFindFiles() {
        System.out.println("\n========== 示例 6: 查找文件 ==========\n");
        
        String result = demo.findFiles(
            "src",
            "*.java",
            "我想找到所有的 Java 源文件"
        );
        
        System.out.println("【查找结果】");
        System.out.println(result);
    }

    @Test
    void testSmartExecute() {
        System.out.println("\n========== 示例 7: 智能执行 ==========\n");
        
        String[] intents = {
            "查看项目的 Java 文件数量",
            "查看最近的 Git 提交记录",
            "检查项目中是否有 TODO 注释"
        };
        
        for (String intent : intents) {
            System.out.println("\n--- 用户意图: " + intent + " ---");
            String result = demo.smartExecute(intent, ".");
            System.out.println(result);
            System.out.println();
        }
    }
}
