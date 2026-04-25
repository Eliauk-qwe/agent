# 路径更新总结

## 更新日期
2026-04-25

## 更新内容

### 1. 文件下载工具路径更新

**文件**: `src/main/java/com/wly/ai_agent_plus/Tool/FileDownloadTool.java`

**修改**:
```java
// 旧路径
private static final String DEFAULT_DOWNLOAD_DIR = "./downloads";

// 新路径
private static final String DEFAULT_DOWNLOAD_DIR = "tmp/download";
```

**影响**:
- 所有通过 `FileDownloadTool` 下载的文件默认保存到 `tmp/download` 目录
- 包括网页抓取、文件下载等功能

### 2. PDF生成工具路径更新

**文件**: `src/main/java/com/wly/ai_agent_plus/Tool/PdfGeneratorTool.java`

**修改**:
```java
// 旧路径
private static final String DEFAULT_OUTPUT_DIR = "generated-documents";

// 新路径
private static final String DEFAULT_OUTPUT_DIR = "tmp/create";
```

**影响**:
- 所有通过 `PdfGeneratorTool` 生成的文档默认保存到 `tmp/create` 目录
- 包括 PDF、Word 文档等

### 3. 测试文件更新

**文件**: `src/test/java/com/wly/ai_agent_plus/Tool/FileDownloadToolTest.java`

**修改内容**:
- 将所有测试中的 `./downloads` 路径改为 `tmp/download`
- 修复了 `fileExists` 和 `getFileInfo` 方法，使用 `File` 对象而不是 `FileUtil`
- 更新测试以使用响应中的实际路径进行验证

**修改的测试方法**:
- `testDownloadWithCustomFileName`
- `testDownloadToFullPath`
- `testFileExists`
- `testGetFileInfo`
- `testBatchDownload`
- `testAutoCreateDirectory`

### 4. 文档更新

**更新的文档**:
1. `PDF_GENERATOR_GUIDE.md` - 更新了默认路径说明
2. `TOOL_PATHS_CONFIG.md` - 新建的路径配置文档
3. `PATH_UPDATE_SUMMARY.md` - 本文档

### 5. Bug 修复

**问题**: `FileDownloadTool` 中的 `fileExists` 和 `getFileInfo` 方法使用 `FileUtil` 导致路径识别问题

**解决方案**: 改用标准的 `java.io.File` 对象

**修改前**:
```java
public boolean fileExists(String path) {
    return FileUtil.exist(path);
}
```

**修改后**:
```java
public boolean fileExists(String path) {
    File file = new File(path);
    return file.exists() && file.isFile();
}
```

## 目录结构

更新后的目录结构：

```
项目根目录/
├── tmp/
│   ├── download/          # 文件下载目录（新）
│   │   ├── *.html        # 下载的网页
│   │   ├── *.pdf         # 下载的PDF
│   │   └── ...           # 其他下载文件
│   │
│   └── create/           # 文件生成目录（新）
│       ├── *.pdf         # 生成的PDF文档
│       ├── *.docx        # 生成的Word文档
│       └── ...           # 其他生成文件
│
└── downloads/            # 旧的下载目录（已废弃）
```

## Git 配置

`.gitignore` 已包含 `tmp/*`，所以临时文件不会被提交到版本控制。

## 迁移指南

### 对于现有代码

如果你的代码中硬编码了旧路径，需要进行以下更新：

**下载工具**:
```java
// 旧代码
request.saveDirectory = "./downloads";

// 新代码
request.saveDirectory = "tmp/download";
// 或者直接使用默认路径（不指定 saveDirectory）
```

**PDF生成工具**:
```java
// 旧代码
pdfGeneratorTool.createSimplePdf(content, "doc", "generated-documents");

// 新代码
pdfGeneratorTool.createSimplePdf(content, "doc", "tmp/create");
// 或者直接使用默认路径（不指定 outputDir）
```

### 清理旧文件

如果需要清理旧的下载目录：

```bash
# 备份旧文件（可选）
mv downloads downloads.backup

# 或直接删除
rm -rf downloads/
rm -rf generated-documents/
```

## 测试验证

运行以下命令验证更新：

```bash
# 运行文件下载工具测试
mvn test -Dtest=FileDownloadToolTest

# 运行PDF生成工具测试
mvn test -Dtest=PdfGeneratorToolTest

# 运行所有测试
mvn test
```

## 注意事项

1. **路径格式**: 新路径使用相对路径格式（不带 `./` 前缀）
2. **自动创建**: 两个工具都会自动创建所需的目录
3. **权限**: 确保应用有权限在 `tmp` 目录下创建文件和子目录
4. **清理**: 建议定期清理 `tmp` 目录以节省磁盘空间
5. **生产环境**: 在生产环境中，考虑使用配置文件管理路径

## 相关文件

- `src/main/java/com/wly/ai_agent_plus/Tool/FileDownloadTool.java`
- `src/main/java/com/wly/ai_agent_plus/Tool/PdfGeneratorTool.java`
- `src/test/java/com/wly/ai_agent_plus/Tool/FileDownloadToolTest.java`
- `src/test/java/com/wly/ai_agent_plus/Tool/PdfGeneratorToolTest.java`
- `PDF_GENERATOR_GUIDE.md`
- `TOOL_PATHS_CONFIG.md`
- `.gitignore`

## 后续改进建议

1. **配置化路径**: 将路径配置移到 `application.yml` 中
2. **环境变量**: 支持通过环境变量覆盖默认路径
3. **自动清理**: 实现定时任务自动清理过期文件
4. **磁盘监控**: 添加磁盘空间监控和告警
5. **云存储**: 考虑集成云存储服务（如 OSS、S3）

## 完成状态

- [x] 更新 FileDownloadTool 默认路径
- [x] 更新 PdfGeneratorTool 默认路径
- [x] 修复 fileExists 和 getFileInfo 方法
- [x] 更新所有相关测试
- [x] 更新文档
- [x] 验证 .gitignore 配置
- [x] 创建路径配置文档
- [x] 创建更新总结文档

## 问题排查

如果遇到文件找不到的问题：

1. **检查路径**: 确认使用的是新路径 `tmp/download` 或 `tmp/create`
2. **检查权限**: 确认应用有权限创建目录和文件
3. **检查日志**: 查看应用日志中的路径信息
4. **手动验证**: 使用 `ls -la tmp/` 检查文件是否真的存在
5. **使用响应路径**: 在测试中使用 `response.savePath` 而不是硬编码路径

## 联系方式

如有问题，请查看相关文档或提交 Issue。
