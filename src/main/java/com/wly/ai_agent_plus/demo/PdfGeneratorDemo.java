package com.wly.ai_agent_plus.demo;

import com.wly.ai_agent_plus.Tool.PdfGeneratorTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PDF 生成工具演示
 * 展示如何使用 PdfGeneratorTool 生成各种格式的文档
 * 
 * 注意：这是一个演示类，需要手动调用 runDemo() 方法来执行演示
 */
@Slf4j
@Component
public class PdfGeneratorDemo {

    /**
     * 运行完整的演示
     */
    public void runDemo(PdfGeneratorTool pdfGeneratorTool) {
            log.info("=== PDF 生成工具演示开始 ===\n");

            // 1. 生成简单的 PDF 文档
            demonstrateSimplePdf(pdfGeneratorTool);

            // 2. 生成格式化的 PDF 文档
            demonstrateFormattedPdf(pdfGeneratorTool);

            // 3. 生成包含表格的 PDF 文档
            demonstratePdfWithTable(pdfGeneratorTool);

            // 4. 从 HTML 生成 PDF
            demonstratePdfFromHtml(pdfGeneratorTool);

            // 5. 生成 Word 文档
            demonstrateWordDocument(pdfGeneratorTool);

            // 6. 生成格式化的 Word 文档
            demonstrateFormattedWordDocument(pdfGeneratorTool);

            log.info("\n=== PDF 生成工具演示结束 ===");
            log.info("所有文档已生成到 '{}' 目录", pdfGeneratorTool.getDefaultOutputDir());
    }

    /**
     * 演示：生成简单的 PDF 文档
     */
    private void demonstrateSimplePdf(PdfGeneratorTool tool) {
        log.info("--- 1. 生成简单的 PDF 文档 ---");
        
        String content = """
                恋爱小贴士
                
                1. 保持真诚：在恋爱中，真诚是最重要的品质。不要试图伪装自己，展现真实的你。
                
                2. 学会倾听：倾听是沟通的艺术。当对方说话时，给予全部的注意力，理解对方的感受。
                
                3. 相互尊重：尊重对方的选择、兴趣和个人空间。健康的关系建立在相互尊重的基础上。
                
                4. 共同成长：一起学习新事物，共同面对挑战，让彼此都变得更好。
                
                5. 保持浪漫：不要让日常琐事磨灭了浪漫。偶尔的惊喜和小礼物能让关系保持新鲜感。
                """;
        
        String filePath = tool.createSimplePdf(content, "恋爱小贴士");
        log.info("✓ 简单 PDF 文档已生成: {}\n", filePath);
    }

    /**
     * 演示：生成格式化的 PDF 文档
     */
    private void demonstrateFormattedPdf(PdfGeneratorTool tool) {
        log.info("--- 2. 生成格式化的 PDF 文档 ---");
        
        String title = "恋爱关系发展指南";
        Map<String, String> sections = new LinkedHashMap<>();
        
        sections.put("第一阶段：初识期", 
                "这是关系的开始阶段，双方开始相互了解。在这个阶段，要保持真诚，展现自己的优点，" +
                "同时也要了解对方的兴趣、价值观和生活方式。不要急于求成，给彼此足够的时间来建立信任。");
        
        sections.put("第二阶段：热恋期", 
                "这是感情最浓烈的时期，双方会频繁互动，渴望了解对方的一切。在享受甜蜜的同时，" +
                "也要保持理性，不要忽视对方的缺点。这个阶段要学会平衡个人生活和恋爱关系。");
        
        sections.put("第三阶段：稳定期", 
                "经过热恋期后，关系进入稳定阶段。这时双方已经建立了深厚的信任和默契。" +
                "要继续保持沟通，分享生活中的点滴，共同规划未来。不要因为关系稳定就忽视对方的感受。");
        
        sections.put("第四阶段：磨合期", 
                "在长期相处中，难免会发现彼此的差异和矛盾。这是关系的考验期，需要双方共同努力。" +
                "学会理解和包容，通过有效沟通解决问题。记住，冲突不是坏事，关键是如何处理。");
        
        sections.put("第五阶段：成熟期", 
                "经过磨合后，关系变得更加成熟和稳固。双方已经学会如何相处，如何处理分歧。" +
                "这个阶段要继续保持新鲜感，共同成长，让关系持续发展。");
        
        String filePath = tool.createFormattedPdf(title, sections, "恋爱关系发展指南");
        log.info("✓ 格式化 PDF 文档已生成: {}\n", filePath);
    }

    /**
     * 演示：生成包含表格的 PDF 文档
     */
    private void demonstratePdfWithTable(PdfGeneratorTool tool) {
        log.info("--- 3. 生成包含表格的 PDF 文档 ---");
        
        String title = "恋爱阶段特征对照表";
        String[] headers = {"阶段", "时间跨度", "主要特征", "注意事项"};
        String[][] data = {
                {"初识期", "1-3个月", "相互了解，建立好感", "保持真诚，不要伪装"},
                {"热恋期", "3-12个月", "感情升温，频繁互动", "享受当下，保持理性"},
                {"稳定期", "1-3年", "关系成熟，相互信任", "维持沟通，共同成长"},
                {"磨合期", "不定期", "发现差异，处理矛盾", "理解包容，有效沟通"},
                {"成熟期", "3年以上", "关系稳固，默契十足", "保持新鲜感，持续发展"}
        };
        
        String filePath = tool.createPdfWithTable(title, headers, data, "恋爱阶段对照表");
        log.info("✓ 表格 PDF 文档已生成: {}\n", filePath);
    }

    /**
     * 演示：从 HTML 生成 PDF
     */
    private void demonstratePdfFromHtml(PdfGeneratorTool tool) {
        log.info("--- 4. 从 HTML 生成 PDF 文档 ---");
        
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: SimSun, serif;
                            line-height: 1.8;
                            padding: 20px;
                        }
                        h1 {
                            color: #e74c3c;
                            text-align: center;
                            border-bottom: 2px solid #e74c3c;
                            padding-bottom: 10px;
                        }
                        h2 {
                            color: #3498db;
                            margin-top: 20px;
                        }
                        p {
                            text-indent: 2em;
                            margin: 10px 0;
                        }
                        .highlight {
                            background-color: #fff3cd;
                            padding: 10px;
                            border-left: 4px solid #ffc107;
                            margin: 15px 0;
                        }
                        ul {
                            margin-left: 2em;
                        }
                        li {
                            margin: 5px 0;
                        }
                    </style>
                </head>
                <body>
                    <h1>恋爱沟通技巧指南</h1>
                    
                    <h2>一、有效沟通的重要性</h2>
                    <p>沟通是维持健康恋爱关系的基石。良好的沟通能够帮助双方更好地理解彼此，
                    及时解决矛盾，增进感情。缺乏沟通或沟通不当往往是导致关系破裂的主要原因。</p>
                    
                    <div class="highlight">
                        <strong>重点提示：</strong>沟通不仅仅是说话，更重要的是倾听和理解。
                    </div>
                    
                    <h2>二、沟通的基本原则</h2>
                    <ul>
                        <li><strong>真诚表达：</strong>诚实地表达自己的想法和感受，不要隐藏或伪装。</li>
                        <li><strong>积极倾听：</strong>给予对方全部的注意力，理解对方的观点。</li>
                        <li><strong>尊重差异：</strong>接受彼此的不同，不要试图改变对方。</li>
                        <li><strong>及时反馈：</strong>对对方的话语给予回应，让对方知道你在倾听。</li>
                        <li><strong>避免指责：</strong>用"我"开头表达感受，而不是用"你"开头指责对方。</li>
                    </ul>
                    
                    <h2>三、处理冲突的技巧</h2>
                    <p>冲突是不可避免的，关键在于如何处理。首先要保持冷静，避免情绪化的反应。
                    其次，要尝试从对方的角度思考问题，理解对方的立场。最后，寻找双赢的解决方案，
                    而不是争论谁对谁错。</p>
                    
                    <h2>四、日常沟通建议</h2>
                    <p>建立定期沟通的习惯，比如每天分享一天的经历和感受。在沟通时，选择合适的时间和地点，
                    避免在疲惫或情绪不佳时讨论重要问题。同时，要学会使用非语言沟通，如眼神交流、
                    肢体接触等，这些都能增进亲密感。</p>
                    
                    <div class="highlight">
                        <strong>温馨提示：</strong>良好的沟通需要双方共同努力，持之以恒。
                    </div>
                </body>
                </html>
                """;
        
        String filePath = tool.createPdfFromHtml(htmlContent, "恋爱沟通技巧指南");
        log.info("✓ HTML 转 PDF 文档已生成: {}\n", filePath);
    }

    /**
     * 演示：生成简单的 Word 文档
     */
    private void demonstrateWordDocument(PdfGeneratorTool tool) {
        log.info("--- 5. 生成简单的 Word 文档 ---");
        
        String content = """
                恋爱中的情绪管理
                
                情绪管理是维持健康恋爱关系的重要技能。以下是一些实用的建议：
                
                1. 认识自己的情绪
                了解自己的情绪状态，识别触发情绪的因素。当你感到愤怒、焦虑或悲伤时，
                先停下来，问问自己为什么会有这样的感受。
                
                2. 学会表达情绪
                不要压抑自己的情绪，但也要学会恰当地表达。使用"我感到..."的句式，
                而不是"你让我..."，这样可以避免指责对方。
                
                3. 给自己冷静的时间
                当情绪激动时，不要急于做出反应。给自己一些时间冷静下来，
                理清思路后再与对方沟通。
                
                4. 寻求支持
                当遇到难以处理的情绪时，可以向朋友、家人或专业人士寻求帮助。
                有时候，倾诉本身就是一种很好的情绪释放方式。
                
                5. 培养积极心态
                保持乐观的态度，多关注关系中的积极方面。感恩对方的付出，
                欣赏彼此的优点，这样能让关系更加和谐。
                
                记住，情绪管理是一个持续的过程，需要不断练习和改进。
                """;
        
        String filePath = tool.createWordDocument(content, "恋爱情绪管理");
        log.info("✓ 简单 Word 文档已生成: {}\n", filePath);
    }

    /**
     * 演示：生成格式化的 Word 文档
     */
    private void demonstrateFormattedWordDocument(PdfGeneratorTool tool) {
        log.info("--- 6. 生成格式化的 Word 文档 ---");
        
        String title = "长期关系维护指南";
        Map<String, String> sections = new LinkedHashMap<>();
        
        sections.put("保持沟通", 
                "定期与伴侣交流，分享彼此的想法、感受和生活经历。不要让日常琐事阻碍了深层次的交流。" +
                "每周至少安排一次专门的沟通时间，讨论关系中的问题和未来的计划。");
        
        sections.put("创造共同回忆", 
                "一起参加活动，创造美好的回忆。可以是旅行、学习新技能、参加兴趣班等。" +
                "共同的经历能够增强彼此的联系，让关系更加牢固。");
        
        sections.put("保持个人空间", 
                "虽然在一起很重要，但也要给彼此足够的个人空间。尊重对方的兴趣爱好和社交需求。" +
                "健康的关系是两个独立个体的结合，而不是完全融为一体。");
        
        sections.put("处理冲突", 
                "冲突是正常的，关键是如何处理。避免冷战，及时沟通解决问题。" +
                "学会妥协和让步，寻找双方都能接受的解决方案。记住，目标是解决问题，而不是赢得争论。");
        
        sections.put("表达爱意", 
                "不要吝啬表达爱意。通过言语、行动和小礼物让对方感受到你的爱。" +
                "简单的'我爱你'、一个拥抱或一份小惊喜都能让关系保持温暖。");
        
        sections.put("共同成长", 
                "支持对方的梦想和目标，一起成长进步。分享彼此的成功和失败，" +
                "在困难时相互扶持，在成功时共同庆祝。让关系成为双方成长的动力。");
        
        String filePath = tool.createFormattedWordDocument(title, sections, "长期关系维护指南");
        log.info("✓ 格式化 Word 文档已生成: {}\n", filePath);
    }
}
