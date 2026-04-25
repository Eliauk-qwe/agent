# PDF 生成工具使用指南

## 概述

本项目集成了基于 iText 7 的 PDF 生成工具，支持生成多种格式的文档（PDF、Word），并完美支持中文字体。

## 功能特性

- ✅ **PDF 生成**：支持生成简单和格式化的 PDF 文档
- ✅ **中文支持**：完美支持中文字体（简体、繁体）
- ✅ **表格支持**：可以在 PDF 中创建表格
- ✅ **HTML 转 PDF**：支持从 HTML 内容生成 PDF
- ✅ **Word 文档**：支持生成 Word 文档（.docx）
- ✅ **格式化**：支持标题、段落、列表等多种格式

## 依赖说明

已添加以下依赖到 `pom.xml`：

```xml
<!-- iText 7 - PDF 生成核心库 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>8.0.5</version>
    <type>pom</type>
</dependency>

<!-- iText 中文字体支持 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>font-asian</artifactId>
    <version>8.0.5</version>
</dependency>

<!-- iText HTML to PDF 转换 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>html2pdf</artifactId>
    <version>5.0.5</version>
</dependency>

<!-- Apache POI - Word 文档生成 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

## 快速开始

### 1. 注入工具类

```java
@Autowired
private PdfGeneratorTool pdfGeneratorTool;
```

### 2. 生成简单的 PDF 文档

```java
String content = "这是一个简单的PDF文档，支持中文！";
String filePath = pdfGeneratorTool.createSimplePdf(content, "my-document");
System.out.println("PDF 已生成: " + filePath);
```

### 3. 生成格式化的 PDF 文档

```java
String title = "恋爱指南";
Map<String, String> sections = new LinkedHashMap<>();
sections.put("第一章：如何开始", "保持真诚，展现真实的自己...");
sections.put("第二章：维持关系", "沟通是关键，定期交流...");

String filePath = pdfGeneratorTool.createFormattedPdf(title, sections, "love-guide");
```

### 4. 生成包含表格的 PDF

```java
String title = "数据统计表";
String[] headers = {"姓名", "年龄", "城市"};
String[][] data = {
    {"张三", "25", "北京"},
    {"李四", "30", "上海"},
    {"王五", "28", "广州"}
};

String filePath = pdfGeneratorTool.createPdfWithTable(title, headers, data, "data-table");
```

### 5. 从 HTML 生成 PDF

```java
String htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <style>
            body { font-family: SimSun; }
            h1 { color: #333; }
        </style>
    </head>
    <body>
        <h1>标题</h1>
        <p>这是一段中文内容。</p>
    </body>
    </html>
    """;

String filePath = pdfGeneratorTool.createPdfFromHtml(htmlContent, "html-document");
```

### 6. 生成 Word 文档

```java
// 简单 Word 文档
String content = "这是一个 Word 文档，支持中文！";
String filePath = pdfGeneratorTool.createWordDocument(content, "my-word-doc");

// 格式化 Word 文档
String title = "工作报告";
Map<String, String> sections = new LinkedHashMap<>();
sections.put("项目概述", "本项目旨在...");
sections.put("进展情况", "目前已完成...");

String filePath = pdfGeneratorTool.createFormattedWordDocument(title, sections, "work-report");
```

## API 参考

### PdfGeneratorTool 类方法

#### 1. createSimplePdf
生成简单的 PDF 文档

```java
String createSimplePdf(String content, String fileName)
String createSimplePdf(String content, String fileName, String outputDir)
```

**参数：**
- `content`: 文档内容
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选，默认为 "tmp/create"）

**返回：** 生成的文件路径

#### 2. createFormattedPdf
生成格式化的 PDF 文档

```java
String createFormattedPdf(String title, Map<String, String> sections, String fileName)
String createFormattedPdf(String title, Map<String, String> sections, String fileName, String outputDir)
```

**参数：**
- `title`: 文档标题
- `sections`: 文档章节（Map<章节标题, 章节内容>）
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选）

**返回：** 生成的文件路径

#### 3. createPdfWithTable
生成包含表格的 PDF 文档

```java
String createPdfWithTable(String title, String[] headers, String[][] data, String fileName)
String createPdfWithTable(String title, String[] headers, String[][] data, String fileName, String outputDir)
```

**参数：**
- `title`: 文档标题
- `headers`: 表格表头
- `data`: 表格数据
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选）

**返回：** 生成的文件路径

#### 4. createPdfFromHtml
从 HTML 生成 PDF

```java
String createPdfFromHtml(String htmlContent, String fileName)
String createPdfFromHtml(String htmlContent, String fileName, String outputDir)
```

**参数：**
- `htmlContent`: HTML 内容
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选）

**返回：** 生成的文件路径

#### 5. createWordDocument
生成简单的 Word 文档

```java
String createWordDocument(String content, String fileName)
String createWordDocument(String content, String fileName, String outputDir)
```

**参数：**
- `content`: 文档内容
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选）

**返回：** 生成的文件路径

#### 6. createFormattedWordDocument
生成格式化的 Word 文档

```java
String createFormattedWordDocument(String title, Map<String, String> sections, String fileName)
String createFormattedWordDocument(String title, Map<String, String> sections, String fileName, String outputDir)
```

**参数：**
- `title`: 文档标题
- `sections`: 文档章节（Map<章节标题, 章节内容>）
- `fileName`: 文件名（不含扩展名）
- `outputDir`: 输出目录（可选）

**返回：** 生成的文件路径

## 运行示例

### 运行测试

```bash
mvn test -Dtest=PdfGeneratorToolTest
```

### 运行演示程序

```bash
mvn spring-boot:run -Dstart-class=com.wly.ai_agent_plus.demo.PdfGeneratorDemo
```

演示程序会生成以下文档：
1. 恋爱小贴士.pdf - 简单 PDF 文档
2. 恋爱关系发展指南.pdf - 格式化 PDF 文档
3. 恋爱阶段对照表.pdf - 包含表格的 PDF 文档
4. 恋爱沟通技巧指南.pdf - 从 HTML 生成的 PDF
5. 恋爱情绪管理.docx - 简单 Word 文档
6. 长期关系维护指南.docx - 格式化 Word 文档

所有文档默认生成到 `tmp/create` 目录。

## 中文字体支持

### PDF 文档
工具使用 iText 内置的 `STSongStd-Light` 字体（宋体），支持：
- 简体中文
- 繁体中文
- 中文标点符号
- 特殊字符

### Word 文档
使用 `宋体` 字体，完美支持中文显示。

### HTML 转 PDF
在 HTML 中指定中文字体：

```html
<style>
    body { font-family: SimSun, serif; }
</style>
```

## 自定义输出目录

```java
// 使用自定义输出目录
String customDir = "my-documents";
String filePath = pdfGeneratorTool.createSimplePdf(content, "test", customDir);

// 使用默认目录（tmp/create）
String filePath = pdfGeneratorTool.createSimplePdf(content, "test");
```

## 注意事项

1. **字体支持**：确保系统中安装了中文字体，或使用 iText 内置的中文字体
2. **文件权限**：确保应用有权限在指定目录创建文件
3. **文件名**：文件名不要包含扩展名，工具会自动添加（.pdf 或 .docx）
4. **HTML 格式**：HTML 转 PDF 时，确保 HTML 格式正确，包含完整的文档结构
5. **表格数据**：表格数据的列数必须与表头列数一致

## 错误处理

所有方法在失败时会抛出 `RuntimeException`，包含详细的错误信息：

```java
try {
    String filePath = pdfGeneratorTool.createSimplePdf(content, "test");
    System.out.println("成功: " + filePath);
} catch (RuntimeException e) {
    System.err.println("生成失败: " + e.getMessage());
}
```

## 高级用法

### 1. 批量生成文档

```java
List<String> contents = Arrays.asList("内容1", "内容2", "内容3");
for (int i = 0; i < contents.size(); i++) {
    String fileName = "document-" + (i + 1);
    pdfGeneratorTool.createSimplePdf(contents.get(i), fileName);
}
```

### 2. 动态生成表格

```java
List<String[]> dataList = fetchDataFromDatabase();
String[][] data = dataList.toArray(new String[0][]);
pdfGeneratorTool.createPdfWithTable("数据报表", headers, data, "report");
```

### 3. 模板化生成

```java
String template = loadTemplate("template.html");
String content = template.replace("{{title}}", title)
                        .replace("{{content}}", content);
pdfGeneratorTool.createPdfFromHtml(content, "templated-doc");
```

## 性能优化建议

1. **批量生成**：如果需要生成大量文档，考虑使用线程池并行处理
2. **缓存字体**：字体加载是耗时操作，可以考虑缓存字体对象
3. **流式处理**：对于大型文档，考虑使用流式 API 减少内存占用
4. **异步生成**：对于 Web 应用，考虑异步生成文档，避免阻塞请求

## 许可证

- iText 7: AGPL 许可证（商业使用需要购买许可证）
- Apache POI: Apache License 2.0

## 相关资源

- [iText 官方文档](https://itextpdf.com/en/resources/api-documentation)
- [Apache POI 文档](https://poi.apache.org/components/document/)
- [iText 中文字体支持](https://kb.itextpdf.com/home/it7kb/ebooks/itext-7-jump-start-tutorial-for-java/chapter-2-introducing-the-pdf-and-document-classes)

## 常见问题

### Q: 生成的 PDF 中文显示为方框？
A: 确保使用了支持中文的字体，如 `STSongStd-Light`。

### Q: 如何自定义字体？
A: 可以使用 `PdfFontFactory.createFont()` 加载自定义字体文件。

### Q: 生成的文件在哪里？
A: 默认在项目根目录的 `generated-documents` 文件夹中。

### Q: 可以生成其他格式吗？
A: 目前支持 PDF 和 Word（.docx）格式。如需其他格式，可以扩展工具类。

### Q: 商业使用需要注意什么？
A: iText 7 使用 AGPL 许可证，商业使用需要购买商业许可证。Apache POI 可以免费商用。
