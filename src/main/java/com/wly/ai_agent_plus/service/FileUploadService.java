package com.wly.ai_agent_plus.service;

import com.wly.ai_agent_plus.entity.UploadedFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传服务
 */
@Service
@Slf4j
public class FileUploadService {
    
    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${file.upload.max-size:10485760}") // 10MB
    private long maxFileSize;
    
    // 内存存储上传的文件信息（生产环境应该使用数据库）
    private final Map<String, UploadedFile> fileStorage = new ConcurrentHashMap<>();
    
    // 支持的非文本文件类型；文本文件会按 text/*、常见文本扩展名或内容特征单独识别。
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
        "image/jpeg", "image",
        "image/png", "image", 
        "image/gif", "image",
        "image/webp", "image",
        "application/pdf", "document",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "document"
    );

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        ".txt", ".md", ".markdown", ".json", ".jsonl", ".csv", ".tsv", ".xml", ".html", ".htm",
        ".css", ".js", ".ts", ".jsx", ".tsx", ".vue", ".java", ".py", ".go", ".rs", ".c", ".cpp",
        ".h", ".hpp", ".cs", ".php", ".rb", ".swift", ".kt", ".kts", ".sql", ".sh", ".bash",
        ".zsh", ".fish", ".bat", ".cmd", ".ps1", ".yaml", ".yml", ".toml", ".ini", ".conf",
        ".properties", ".env", ".log", ".rtf", ".srt", ".vtt", ".tex", ".gradle", ".gitignore"
    );
    
    /**
     * 上传文件
     */
    public UploadedFile uploadFile(MultipartFile file, String sessionId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 创建上传目录
        createUploadDirectory();
        
        // 生成存储文件名
        String storedName = generateStoredName(file.getOriginalFilename());
        String filePath = uploadPath + File.separator + storedName;
        
        // 保存文件
        Path targetPath = Paths.get(filePath);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // 创建文件记录
        UploadedFile uploadedFile = new UploadedFile(
            file.getOriginalFilename(),
            storedName,
            filePath,
            determineFileType(file),
            file.getContentType(),
            file.getSize()
        );
        uploadedFile.setSessionId(sessionId);
        
        // 提取文本内容
        try {
            String extractedText = extractTextContent(file, filePath);
            uploadedFile.setExtractedText(extractedText);
        } catch (Exception e) {
            log.warn("无法提取文件文本内容: {}", e.getMessage());
        }
        
        // 生成ID并存储
        String fileId = UUID.randomUUID().toString();
        long numericId = Long.valueOf(fileId.hashCode());
        uploadedFile.setId(numericId);
        // 使用数字ID作为存储key，便于查找
        fileStorage.put(String.valueOf(numericId), uploadedFile);
        
        // 同时保留UUID映射
        fileStorage.put("uuid:" + fileId, uploadedFile);
        
        log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), storedName);
        return uploadedFile;
    }
    
    /**
     * 获取文件信息 - 支持数字ID和UUID
     */
    public UploadedFile getFile(String fileId) {
        // 尝试直接查找（数字ID）
        UploadedFile file = fileStorage.get(fileId);
        if (file != null) {
            return file;
        }
        // 尝试通过UUID查找
        return fileStorage.get("uuid:" + fileId);
    }
    
    /**
     * 获取会话的所有文件
     */
    public List<UploadedFile> getFilesBySession(String sessionId) {
        return new LinkedHashSet<>(fileStorage.values()).stream()
            .filter(file -> sessionId.equals(file.getSessionId()))
            .sorted((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()))
            .toList();
    }
    
    /**
     * 删除文件
     */
    public boolean deleteFile(String fileId) {
        UploadedFile file = getFile(fileId);
        if (file != null) {
            try {
                // 删除物理文件
                Files.deleteIfExists(Paths.get(file.getFilePath()));
                // 删除记录（删除两个key）
                fileStorage.remove(fileId);
                fileStorage.values().removeIf(f -> f.getId().equals(file.getId()));
                log.info("文件删除成功: {}", file.getOriginalName());
                return true;
            } catch (IOException e) {
                log.error("删除文件失败: {}", e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        if (!isSupportedFile(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
    }

    private boolean isSupportedFile(MultipartFile file) throws IOException {
        String contentType = normalizeContentType(file.getContentType());
        return SUPPORTED_TYPES.containsKey(contentType) || isTextFile(file, contentType);
    }

    private String determineFileType(MultipartFile file) throws IOException {
        String contentType = normalizeContentType(file.getContentType());
        if (isTextFile(file, contentType)) {
            return "text";
        }
        return SUPPORTED_TYPES.getOrDefault(contentType, "file");
    }

    private boolean isTextFile(MultipartFile file, String contentType) throws IOException {
        if (contentType != null && (contentType.startsWith("text/") || isTextLikeApplicationType(contentType))) {
            return true;
        }
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.contains(extension) || looksLikeText(file);
    }

    private boolean isTextLikeApplicationType(String contentType) {
        return contentType.equals("application/json")
                || contentType.equals("application/xml")
                || contentType.equals("application/yaml")
                || contentType.equals("application/x-yaml")
                || contentType.equals("application/toml")
                || contentType.equals("application/javascript")
                || contentType.equals("application/x-javascript")
                || contentType.equals("application/sql")
                || contentType.endsWith("+json")
                || contentType.endsWith("+xml");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeText(MultipartFile file) throws IOException {
        byte[] sample = new byte[(int) Math.min(file.getSize(), 4096)];
        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.read(sample);
            if (read <= 0) {
                return true;
            }
            int suspiciousBytes = 0;
            for (int i = 0; i < read; i++) {
                int value = sample[i] & 0xff;
                if (value == 0) {
                    return false;
                }
                if (value < 0x09 || (value > 0x0d && value < 0x20)) {
                    suspiciousBytes++;
                }
            }
            return suspiciousBytes < read * 0.05;
        }
    }
    
    /**
     * 创建上传目录
     */
    private void createUploadDirectory() throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }
    
    /**
     * 生成存储文件名
     */
    private String generateStoredName(String originalName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalName);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + extension;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    /**
     * 提取文本内容
     */
    private String extractTextContent(MultipartFile file, String filePath) throws IOException {
        String contentType = normalizeContentType(file.getContentType());

        if (isTextFile(file, contentType)) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        
        switch (contentType) {
            case "application/pdf":
                return extractPdfText(filePath);
                
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return extractWordText(file);
                
            default:
                if (contentType != null && contentType.startsWith("image/")) {
                    return "图片文件: " + file.getOriginalFilename();
                }
                return "无法提取文本内容";
        }
    }
    
    /**
     * 提取PDF文本
     */
    private String extractPdfText(String filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * 提取Word文本
     */
    private String extractWordText(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
