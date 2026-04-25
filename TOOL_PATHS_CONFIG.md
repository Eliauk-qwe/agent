# 工具路径配置说明

## 概述

本项目中的各种工具使用统一的路径规范，所有临时文件和生成文件都存储在 `tmp` 目录下。

## 目录结构

```
tmp/
├── download/          # 文件下载目录
│   ├── *.html        # 下载的网页文件
│   ├── *.pdf         # 下载的PDF文件
│   └── ...           # 其他下载的文件
│
└── create/           # 文件生成目录
    ├── *.pdf         # 生成的PDF文档
    ├── *.docx        # 生成的Word文档
    └── ...           # 其他生成的文件
```

## 工具路径配置

### 1. 文件下载工具 (FileDownloadTool)

**默认路径**: `tmp/download`

**用途**: 
- 下载网页内容
- 下载网络资源
- 保存抓取的文件

**示例**:
```java
// 使用默认路径
FileDownloadTool.Request request = new FileDownloadTool.Request(url);
Response response = fileDownloadTool.apply(request);
// 文件保存到: tmp/download/文件名

// 自定义路径
request.saveDirectory = "custom/path";
Response response = fileDownloadTool.apply(request);
// 文件保存到: custom/path/文件名
```

### 2. PDF生成工具 (PdfGeneratorTool)

**默认路径**: `tmp/create`

**用途**:
- 生成PDF文档
- 生成Word文档
- HTML转PDF

**示例**:
```java
// 使用默认路径
String filePath = pdfGeneratorTool.createSimplePdf(content, "document");
// 文件保存到: tmp/create/document.pdf

// 自定义路径
String filePath = pdfGeneratorTool.createSimplePdf(content, "document", "custom/path");
// 文件保存到: custom/path/document.pdf
```

## 路径常量

### FileDownloadTool
```java
private static final String DEFAULT_DOWNLOAD_DIR = "tmp/download";
```

### PdfGeneratorTool
```java
private static final String DEFAULT_OUTPUT_DIR = "tmp/create";
```

## 目录管理

### 自动创建
所有工具都会自动创建所需的目录，无需手动创建。

```java
// 目录不存在时会自动创建
Path dirPath = Paths.get(dir);
if (!Files.exists(dirPath)) {
    Files.createDirectories(dirPath);
}
```

### 清理建议

定期清理临时文件以节省磁盘空间：

```bash
# 清理下载目录
rm -rf tmp/download/*

# 清理生成目录
rm -rf tmp/create/*

# 清理整个tmp目录
rm -rf tmp/*
```

## Git 配置

建议将 `tmp` 目录添加到 `.gitignore`：

```gitignore
# 临时文件目录
tmp/
tmp/download/
tmp/create/
```

## 路径修改指南

如果需要修改默认路径，请按以下步骤操作：

### 1. 修改工具类常量

**FileDownloadTool.java**:
```java
private static final String DEFAULT_DOWNLOAD_DIR = "your/custom/download/path";
```

**PdfGeneratorTool.java**:
```java
private static final String DEFAULT_OUTPUT_DIR = "your/custom/create/path";
```

### 2. 更新相关文档

- `PDF_GENERATOR_GUIDE.md`
- `WEB_SCRAPER_GUIDE.md`
- `TOOL_PATHS_CONFIG.md`（本文档）

### 3. 更新测试用例

确保测试用例中的路径断言与新路径一致。

### 4. 更新 .gitignore

如果使用新的路径，记得更新 `.gitignore` 文件。

## 最佳实践

### 1. 使用相对路径
```java
// 推荐：相对路径
String path = "tmp/download/file.html";

// 不推荐：绝对路径
String path = "/home/user/project/tmp/download/file.html";
```

### 2. 路径分隔符
使用 `File.separator` 或 `Paths.get()` 确保跨平台兼容：

```java
// 推荐
String path = "tmp" + File.separator + "download" + File.separator + "file.html";
// 或
Path path = Paths.get("tmp", "download", "file.html");

// 不推荐（仅适用于Unix/Linux）
String path = "tmp/download/file.html";
```

### 3. 检查文件是否存在
```java
File file = new File(path);
if (file.exists()) {
    // 文件已存在，决定是覆盖还是跳过
}
```

### 4. 处理文件名冲突
```java
// 添加时间戳避免冲突
String fileName = "document_" + System.currentTimeMillis() + ".pdf";

// 或添加序号
int counter = 1;
String fileName = "document.pdf";
while (new File(dir + "/" + fileName).exists()) {
    fileName = "document_" + counter + ".pdf";
    counter++;
}
```

## 权限要求

确保应用程序有权限在指定目录创建和写入文件：

```bash
# Linux/Mac: 设置目录权限
chmod 755 tmp/
chmod 755 tmp/download/
chmod 755 tmp/create/

# 或递归设置
chmod -R 755 tmp/
```

## 磁盘空间监控

建议监控 `tmp` 目录的磁盘使用情况：

```bash
# 查看目录大小
du -sh tmp/

# 查看子目录大小
du -sh tmp/*/

# 查看详细信息
du -h tmp/ | sort -h
```

## 常见问题

### Q: 为什么使用 tmp 目录？
A: `tmp` 是临时文件的标准目录名，便于识别和管理临时文件。

### Q: 可以使用绝对路径吗？
A: 可以，但不推荐。相对路径更灵活，便于项目迁移。

### Q: 如何在生产环境中配置路径？
A: 建议通过配置文件（如 `application.yml`）配置路径，而不是硬编码。

### Q: tmp 目录会自动清理吗？
A: 不会。需要手动清理或编写定时任务清理。

### Q: 如何防止磁盘空间耗尽？
A: 
1. 定期清理旧文件
2. 设置文件大小限制
3. 监控磁盘使用情况
4. 实现文件过期自动删除机制

## 未来改进

考虑实现以下功能：

1. **配置化路径**: 通过配置文件管理路径
2. **自动清理**: 定期清理过期文件
3. **磁盘配额**: 限制目录最大占用空间
4. **文件归档**: 自动归档旧文件
5. **云存储集成**: 支持上传到云存储服务

## 相关文档

- [PDF生成工具使用指南](PDF_GENERATOR_GUIDE.md)
- [网页抓取工具使用指南](WEB_SCRAPER_GUIDE.md)
- [文件下载工具使用指南](FILE_DOWNLOAD_GUIDE.md)
