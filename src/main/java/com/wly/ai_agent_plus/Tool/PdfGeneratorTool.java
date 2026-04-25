package com.wly.ai_agent_plus.Tool;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * PDF 生成工具
 * 支持多种格式生成，包括 PDF、Word 等
 * 支持中文字体
 */
@Slf4j
@Component
public class PdfGeneratorTool {

    // 默认输出目录
    private static final String DEFAULT_OUTPUT_DIR = "tmp/create";
    
    // 支持的文件格式
    public enum FileFormat {
        PDF, DOCX, HTML
    }

    /**
     * 获取中文字体
     * 尝试多种字体方案，确保兼容性
     */
    private PdfFont getChineseFont() throws Exception {
        // 方案1: 尝试使用系统中文字体
        String[] fontPaths = {
            // Linux 常见字体路径
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            // Windows 字体路径
            "C:/Windows/Fonts/msyh.ttc,0",  // 微软雅黑
            "C:/Windows/Fonts/simsun.ttc,0", // 宋体
            // Mac 字体路径
            "/System/Library/Fonts/PingFang.ttc,0",
            "/Library/Fonts/Arial Unicode.ttf"
        };
        
        for (String fontPath : fontPaths) {
            try {
                File fontFile = new File(fontPath.split(",")[0]);
                if (fontFile.exists()) {
                    log.debug("使用字体: {}", fontPath);
                    return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                }
            } catch (Exception e) {
                log.debug("无法加载字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 方案2: 使用 Helvetica（不支持中文，但不会报错）
        log.warn("未找到中文字体，使用 Helvetica（中文可能显示为方框）");
        return PdfFontFactory.createFont("Helvetica", PdfEncodings.WINANSI);
    }

    /**
     * 创建简单的 PDF 文档（支持中文）
     *
     * @param content  文档内容
     * @param fileName 文件名（不含扩展名）
     * @return 生成的文件路径
     */
    @Tool(name = "createSimplePdf", description = "Create a simple PDF document with Chinese font support. File will be saved to tmp/create directory.")
    public String createSimplePdf(@ToolParam(description = "Content of the PDF document") String content, 
                                  @ToolParam(description = "File name without extension") String fileName) {
        return createSimplePdf(content, fileName, null);
    }

    /**
     * 创建简单的 PDF 文档（支持中文）
     *
     * @param content    文档内容
     * @param fileName   文件名（不含扩展名）
     * @param outputDir  输出目录（可选）
     * @return 生成的文件路径
     */
    public String createSimplePdf(String content, String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".pdf";
            
            // 创建 PDF 写入器
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // 获取中文字体
            PdfFont font = getChineseFont();

            // 添加内容
            Paragraph paragraph = new Paragraph(content)
                    .setFont(font)
                    .setFontSize(12);
            document.add(paragraph);

            document.close();
            
            log.info("PDF 文档生成成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("生成 PDF 文档失败", e);
            throw new RuntimeException("生成 PDF 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建格式化的 PDF 文档（支持标题、段落、列表等）
     *
     * @param title      文档标题
     * @param sections   文档章节（Map<章节标题, 章节内容>）
     * @param fileName   文件名（不含扩展名）
     * @return 生成的文件路径
     */
    public String createFormattedPdf(String title, Map<String, String> sections, String fileName) {
        return createFormattedPdf(title, sections, fileName, null);
    }

    /**
     * 创建格式化的 PDF 文档（支持标题、段落、列表等）
     *
     * @param title      文档标题
     * @param sections   文档章节（Map<章节标题, 章节内容>）
     * @param fileName   文件名（不含扩展名）
     * @param outputDir  输出目录（可选）
     * @return 生成的文件路径
     */
    public String createFormattedPdf(String title, Map<String, String> sections, 
                                    String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".pdf";
            
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // 获取中文字体
            PdfFont font = getChineseFont();

            // 添加标题
            Paragraph titleParagraph = new Paragraph(title)
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(titleParagraph);

            // 添加章节
            for (Map.Entry<String, String> section : sections.entrySet()) {
                // 章节标题
                Paragraph sectionTitle = new Paragraph(section.getKey())
                        .setFont(font)
                        .setFontSize(16)
                        .setBold()
                        .setMarginTop(15)
                        .setMarginBottom(10);
                document.add(sectionTitle);

                // 章节内容
                Paragraph sectionContent = new Paragraph(section.getValue())
                        .setFont(font)
                        .setFontSize(12)
                        .setMarginBottom(10);
                document.add(sectionContent);
            }

            document.close();
            
            log.info("格式化 PDF 文档生成成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("生成格式化 PDF 文档失败", e);
            throw new RuntimeException("生成格式化 PDF 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建包含表格的 PDF 文档
     *
     * @param title      文档标题
     * @param headers    表格表头
     * @param data       表格数据
     * @param fileName   文件名（不含扩展名）
     * @return 生成的文件路径
     */
    public String createPdfWithTable(String title, String[] headers, String[][] data, String fileName) {
        return createPdfWithTable(title, headers, data, fileName, null);
    }

    /**
     * 创建包含表格的 PDF 文档
     *
     * @param title      文档标题
     * @param headers    表格表头
     * @param data       表格数据
     * @param fileName   文件名（不含扩展名）
     * @param outputDir  输出目录（可选）
     * @return 生成的文件路径
     */
    public String createPdfWithTable(String title, String[] headers, String[][] data, 
                                    String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".pdf";
            
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // 获取中文字体
            PdfFont font = getChineseFont();

            // 添加标题
            Paragraph titleParagraph = new Paragraph(title)
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(titleParagraph);

            // 创建表格
            Table table = new Table(UnitValue.createPercentArray(headers.length))
                    .useAllAvailableWidth();

            // 添加表头
            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setFont(font).setBold())
                        .setBackgroundColor(new DeviceRgb(200, 200, 200))
                        .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(headerCell);
            }

            // 添加数据行
            for (String[] row : data) {
                for (String cell : row) {
                    table.addCell(new Cell()
                            .add(new Paragraph(cell).setFont(font))
                            .setTextAlignment(TextAlignment.CENTER));
                }
            }

            document.add(table);
            document.close();
            
            log.info("包含表格的 PDF 文档生成成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("生成包含表格的 PDF 文档失败", e);
            throw new RuntimeException("生成包含表格的 PDF 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 HTML 生成 PDF
     *
     * @param htmlContent HTML 内容
     * @param fileName    文件名（不含扩展名）
     * @return 生成的文件路径
     */
    public String createPdfFromHtml(String htmlContent, String fileName) {
        return createPdfFromHtml(htmlContent, fileName, null);
    }

    /**
     * 从 HTML 生成 PDF
     *
     * @param htmlContent HTML 内容
     * @param fileName    文件名（不含扩展名）
     * @param outputDir   输出目录（可选）
     * @return 生成的文件路径
     */
    public String createPdfFromHtml(String htmlContent, String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".pdf";
            
            // 配置转换属性（支持中文）
            ConverterProperties properties = new ConverterProperties();
            
            // 转换 HTML 到 PDF
            HtmlConverter.convertToPdf(htmlContent, new FileOutputStream(filePath), properties);
            
            log.info("从 HTML 生成 PDF 文档成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("从 HTML 生成 PDF 文档失败", e);
            throw new RuntimeException("从 HTML 生成 PDF 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 Word 文档（支持中文）
     *
     * @param content  文档内容
     * @param fileName 文件名（不含扩展名）
     * @return 生成的文件路径
     */
    @Tool(name = "createWordDocument", description = "Create a Word document (.docx) with Chinese font support. File will be saved to tmp/create directory.")
    public String createWordDocument(@ToolParam(description = "Content of the Word document") String content, 
                                     @ToolParam(description = "File name without extension") String fileName) {
        return createWordDocument(content, fileName, null);
    }

    /**
     * 创建 Word 文档（支持中文）
     *
     * @param content   文档内容
     * @param fileName  文件名（不含扩展名）
     * @param outputDir 输出目录（可选）
     * @return 生成的文件路径
     */
    public String createWordDocument(String content, String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".docx";
            
            // 创建 Word 文档
            XWPFDocument document = new XWPFDocument();
            
            // 添加段落
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(content);
            run.setFontSize(12);
            run.setFontFamily("宋体"); // 设置中文字体

            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
            
            document.close();
            
            log.info("Word 文档生成成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("生成 Word 文档失败", e);
            throw new RuntimeException("生成 Word 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建格式化的 Word 文档
     *
     * @param title    文档标题
     * @param sections 文档章节
     * @param fileName 文件名（不含扩展名）
     * @return 生成的文件路径
     */
    public String createFormattedWordDocument(String title, Map<String, String> sections, String fileName) {
        return createFormattedWordDocument(title, sections, fileName, null);
    }

    /**
     * 创建格式化的 Word 文档
     *
     * @param title     文档标题
     * @param sections  文档章节
     * @param fileName  文件名（不含扩展名）
     * @param outputDir 输出目录（可选）
     * @return 生成的文件路径
     */
    public String createFormattedWordDocument(String title, Map<String, String> sections, 
                                             String fileName, String outputDir) {
        try {
            // 确保输出目录存在
            String dir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = dir + "/" + fileName + ".docx";
            
            XWPFDocument document = new XWPFDocument();
            
            // 添加标题
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(title);
            titleRun.setFontSize(20);
            titleRun.setBold(true);
            titleRun.setFontFamily("宋体");
            titleParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);

            // 添加空行
            document.createParagraph();

            // 添加章节
            for (Map.Entry<String, String> section : sections.entrySet()) {
                // 章节标题
                XWPFParagraph sectionTitleParagraph = document.createParagraph();
                XWPFRun sectionTitleRun = sectionTitleParagraph.createRun();
                sectionTitleRun.setText(section.getKey());
                sectionTitleRun.setFontSize(16);
                sectionTitleRun.setBold(true);
                sectionTitleRun.setFontFamily("宋体");

                // 章节内容
                XWPFParagraph sectionContentParagraph = document.createParagraph();
                XWPFRun sectionContentRun = sectionContentParagraph.createRun();
                sectionContentRun.setText(section.getValue());
                sectionContentRun.setFontSize(12);
                sectionContentRun.setFontFamily("宋体");

                // 添加空行
                document.createParagraph();
            }

            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
            
            document.close();
            
            log.info("格式化 Word 文档生成成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("生成格式化 Word 文档失败", e);
            throw new RuntimeException("生成格式化 Word 文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取默认输出目录
     */
    public String getDefaultOutputDir() {
        return DEFAULT_OUTPUT_DIR;
    }
}
