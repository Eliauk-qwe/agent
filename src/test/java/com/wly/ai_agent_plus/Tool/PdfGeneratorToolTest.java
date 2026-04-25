package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDF 生成工具测试类
 */
class PdfGeneratorToolTest {

    private PdfGeneratorTool pdfGeneratorTool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        pdfGeneratorTool = new PdfGeneratorTool();
    }

    @Test
    void testCreateSimplePdf() {
        // 准备测试数据
        String content = "这是一个简单的PDF文档测试。\n支持中文字符！\nThis is a test with English text.";
        String fileName = "simple-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createSimplePdf(content, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".pdf"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("生成的简单 PDF 文件: " + filePath);
    }

    @Test
    void testCreateFormattedPdf() {
        // 准备测试数据
        String title = "恋爱指南文档";
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("第一章：如何开始一段恋爱", 
                "恋爱是人生中美好的经历。首先要保持真诚，展现真实的自己。" +
                "其次要学会倾听，理解对方的想法和感受。");
        sections.put("第二章：维持长久关系的秘诀", 
                "沟通是关键。定期交流彼此的想法和感受，及时解决矛盾。" +
                "同时要给予对方足够的空间和信任。");
        sections.put("第三章：处理冲突的艺术", 
                "冲突是不可避免的，重要的是如何处理。保持冷静，避免情绪化。" +
                "尝试从对方的角度思考问题，寻找双赢的解决方案。");
        
        String fileName = "formatted-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createFormattedPdf(title, sections, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".pdf"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("生成的格式化 PDF 文件: " + filePath);
    }

    @Test
    void testCreatePdfWithTable() {
        // 准备测试数据
        String title = "恋爱阶段对照表";
        String[] headers = {"阶段", "特征", "建议"};
        String[][] data = {
                {"初识期", "相互了解，建立好感", "保持真诚，展现优点"},
                {"热恋期", "感情升温，频繁互动", "享受当下，保持新鲜感"},
                {"稳定期", "关系成熟，相互信任", "维持沟通，共同成长"},
                {"磨合期", "发现差异，处理矛盾", "理解包容，寻求共识"}
        };
        String fileName = "table-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createPdfWithTable(title, headers, data, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".pdf"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("生成的表格 PDF 文件: " + filePath);
    }

    @Test
    void testCreatePdfFromHtml() {
        // 准备测试数据
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: SimSun; }
                        h1 { color: #333; text-align: center; }
                        p { line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <h1>恋爱技巧分享</h1>
                    <h2>1. 沟通的重要性</h2>
                    <p>良好的沟通是维持关系的基础。要学会表达自己的想法，同时倾听对方的声音。</p>
                    <h2>2. 相互尊重</h2>
                    <p>尊重对方的选择和决定，给予足够的个人空间。</p>
                    <h2>3. 共同成长</h2>
                    <p>一起学习新事物，共同进步，让关系更加稳固。</p>
                </body>
                </html>
                """;
        String fileName = "html-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createPdfFromHtml(htmlContent, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".pdf"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("从 HTML 生成的 PDF 文件: " + filePath);
    }

    @Test
    void testCreateWordDocument() {
        // 准备测试数据
        String content = "这是一个简单的 Word 文档测试。\n支持中文字符！\nThis is a test with English text.";
        String fileName = "word-simple-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createWordDocument(content, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".docx"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("生成的简单 Word 文件: " + filePath);
    }

    @Test
    void testCreateFormattedWordDocument() {
        // 准备测试数据
        String title = "恋爱指南文档";
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("第一章：如何开始一段恋爱", 
                "恋爱是人生中美好的经历。首先要保持真诚，展现真实的自己。" +
                "其次要学会倾听，理解对方的想法和感受。");
        sections.put("第二章：维持长久关系的秘诀", 
                "沟通是关键。定期交流彼此的想法和感受，及时解决矛盾。" +
                "同时要给予对方足够的空间和信任。");
        sections.put("第三章：处理冲突的艺术", 
                "冲突是不可避免的，重要的是如何处理。保持冷静，避免情绪化。" +
                "尝试从对方的角度思考问题，寻找双赢的解决方案。");
        
        String fileName = "word-formatted-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createFormattedWordDocument(title, sections, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".docx"));
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("生成的格式化 Word 文件: " + filePath);
    }

    @Test
    void testDefaultOutputDirectory() {
        // 测试使用默认输出目录
        String content = "测试默认输出目录";
        String fileName = "default-dir-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createSimplePdf(content, fileName);

        // 验证结果
        assertNotNull(filePath);
        assertTrue(filePath.contains(pdfGeneratorTool.getDefaultOutputDir()));
        File file = new File(filePath);
        assertTrue(file.exists());
        
        System.out.println("使用默认目录生成的文件: " + filePath);
        
        // 清理测试文件
        file.delete();
    }

    @Test
    void testChineseCharacterSupport() {
        // 测试基本中文字符（避免使用特殊符号）
        String content = """
                测试中文字符支持：
                简体中文：你好世界
                数字：一二三四五六七八九十
                常用标点：，。！？
                """;
        String fileName = "chinese-char-test";

        // 执行测试
        String filePath = pdfGeneratorTool.createSimplePdf(content, fileName, tempDir.toString());

        // 验证结果
        assertNotNull(filePath);
        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        
        System.out.println("中文字符测试文件: " + filePath);
    }
}
