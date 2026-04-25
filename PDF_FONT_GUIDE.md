# PDF 中文字体配置指南

## 问题说明

在使用 iText 生成 PDF 时，中文字体支持是一个常见问题。本文档说明了如何配置和使用中文字体。

## 字体方案

### 方案 1: 使用系统字体（推荐）

`PdfGeneratorTool` 会自动尝试加载系统中的中文字体，按以下顺序查找：

#### Linux 系统
```bash
# Noto Sans CJK（推荐）
/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc
/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc

# 文泉驿微米黑
/usr/share/fonts/truetype/wqy/wqy-microhei.ttc

# AR PL UMing
/usr/share/fonts/truetype/arphic/uming.ttc
```

#### Windows 系统
```
C:/Windows/Fonts/msyh.ttc    # 微软雅黑
C:/Windows/Fonts/simsun.ttc  # 宋体
```

#### macOS 系统
```
/System/Library/Fonts/PingFang.ttc      # 苹方
/Library/Fonts/Arial Unicode.ttf        # Arial Unicode
```

### 方案 2: 安装中文字体

如果系统没有中文字体，需要手动安装：

#### Ubuntu/Debian
```bash
# 安装 Noto Sans CJK（推荐）
sudo apt-get update
sudo apt-get install fonts-noto-cjk

# 或安装文泉驿字体
sudo apt-get install fonts-wqy-microhei fonts-wqy-zenhei

# 刷新字体缓存
fc-cache -fv
```

#### CentOS/RHEL
```bash
# 安装字体
sudo yum install google-noto-sans-cjk-fonts
# 或
sudo yum install wqy-microhei-fonts

# 刷新字体缓存
fc-cache -fv
```

#### 验证字体安装
```bash
# 列出所有中文字体
fc-list :lang=zh

# 查找特定字体
fc-list | grep -i noto
fc-list | grep -i wqy
```

### 方案 3: 使用自定义字体文件

如果需要使用特定的字体文件：

1. 将字体文件放到项目的 `resources/fonts/` 目录
2. 修改 `PdfGeneratorTool.java` 中的 `getChineseFont()` 方法：

```java
private PdfFont getChineseFont() throws Exception {
    // 从 classpath 加载字体
    String fontPath = "fonts/your-font.ttf";
    InputStream fontStream = getClass().getClassLoader()
        .getResourceAsStream(fontPath);
    
    if (fontStream != null) {
        byte[] fontBytes = fontStream.readAllBytes();
        return PdfFontFactory.createFont(fontBytes, 
            PdfEncodings.IDENTITY_H, 
            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    }
    
    // 回退方案
    return PdfFontFactory.createFont("Helvetica", PdfEncodings.WINANSI);
}
```

## 字体限制

### Helvetica 字体（默认回退）
- **不支持中文**：中文字符会显示为方框（□）
- **仅支持**：英文、数字、基本标点符号
- **使用场景**：纯英文文档或临时测试

### 中文字体支持的字符
- 简体中文
- 繁体中文
- 日文假名
- 韩文
- 英文、数字
- 常用标点符号

### 特殊字符注意事项
某些特殊符号可能不被所有字体支持：
- ©®™ (版权符号)
- €¥£ (货币符号)
- ←→↑↓ (箭头)
- ★☆♥♦ (特殊符号)

如果需要使用这些符号，建议：
1. 使用 Noto Sans CJK（支持最广泛）
2. 或使用 HTML 转 PDF 方式
3. 或使用 Word 文档格式

## 当前实现

`PdfGeneratorTool` 的字体加载策略：

```java
private PdfFont getChineseFont() throws Exception {
    // 1. 尝试加载系统中文字体
    String[] fontPaths = {
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
        "C:/Windows/Fonts/msyh.ttc,0",
        "/System/Library/Fonts/PingFang.ttc,0",
        // ... 更多路径
    };
    
    for (String fontPath : fontPaths) {
        if (字体文件存在) {
            return 加载该字体;
        }
    }
    
    // 2. 如果没有找到中文字体，使用 Helvetica
    log.warn("未找到中文字体，使用 Helvetica（中文可能显示为方框）");
    return PdfFontFactory.createFont("Helvetica", PdfEncodings.WINANSI);
}
```

## 测试字体支持

### 方法 1: 运行测试
```bash
mvn test -Dtest=PdfGeneratorToolTest#testChineseCharacterSupport
```

### 方法 2: 手动测试
```java
PdfGeneratorTool tool = new PdfGeneratorTool();
String content = "测试中文：你好世界！Hello World!";
String filePath = tool.createSimplePdf(content, "test");
System.out.println("生成文件: " + filePath);
```

打开生成的 PDF 文件：
- 如果中文正常显示 → 字体配置成功
- 如果中文显示为方框 → 需要安装中文字体

## 日志信息

查看应用日志了解字体加载情况：

```
# 成功加载中文字体
DEBUG - 使用字体: /usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0

# 未找到中文字体
WARN - 未找到中文字体，使用 Helvetica（中文可能显示为方框）
```

## Docker 环境

如果在 Docker 容器中运行，需要在 Dockerfile 中安装字体：

```dockerfile
FROM openjdk:21-slim

# 安装中文字体
RUN apt-get update && \
    apt-get install -y fonts-noto-cjk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 复制应用
COPY target/app.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 常见问题

### Q: 为什么中文显示为方框？
A: 系统没有安装中文字体。请按照上述方法安装字体。

### Q: 如何确认字体是否安装成功？
A: 运行 `fc-list :lang=zh` 查看已安装的中文字体。

### Q: 可以使用 iText 内置的中文字体吗？
A: iText 7 的 `font-asian` 包提供了 `STSongStd-Light` 等字体，但在某些特殊字符上可能有兼容性问题。推荐使用系统字体。

### Q: Word 文档支持中文吗？
A: 是的，Apache POI 生成的 Word 文档完全支持中文，不需要额外配置。

### Q: HTML 转 PDF 支持中文吗？
A: 支持，但需要在 HTML 中指定中文字体：
```html
<style>
    body { font-family: 'Noto Sans CJK SC', SimSun, serif; }
</style>
```

### Q: 生产环境推荐使用哪种字体？
A: 推荐使用 Noto Sans CJK，因为：
- 开源免费
- 支持简繁日韩
- 字符覆盖广
- 跨平台兼容

## 性能优化

### 字体缓存
如果频繁生成 PDF，可以缓存字体对象：

```java
private static PdfFont cachedFont = null;

private PdfFont getChineseFont() throws Exception {
    if (cachedFont == null) {
        cachedFont = loadFont();
    }
    return cachedFont;
}
```

### 字体子集化
iText 会自动进行字体子集化，只嵌入使用到的字符，减小文件大小。

## 相关资源

- [iText 官方文档 - 字体](https://kb.itextpdf.com/home/it7kb/ebooks/itext-7-jump-start-tutorial-for-java/chapter-2-introducing-the-pdf-and-document-classes)
- [Noto Fonts 官网](https://www.google.com/get/noto/)
- [文泉驿字体](http://wenq.org/)
- [Apache POI 文档](https://poi.apache.org/)

## 总结

1. **优先使用系统字体**：自动检测，无需额外配置
2. **安装 Noto Sans CJK**：最佳的开源中文字体方案
3. **Word 格式**：如果 PDF 字体问题难以解决，可以使用 Word 格式
4. **测试验证**：生成测试文档确认字体正常工作
