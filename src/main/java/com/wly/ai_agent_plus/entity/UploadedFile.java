package com.wly.ai_agent_plus.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上传文件实体类
 */
@Data
public class UploadedFile {
    private Long id;
    private String originalName;    // 原始文件名
    private String storedName;      // 存储文件名
    private String filePath;        // 文件路径
    private String fileType;        // 文件类型 (image, document, text, etc.)
    private String mimeType;        // MIME类型
    private Long fileSize;          // 文件大小（字节）
    private String extractedText;   // 提取的文本内容（用于AI分析）
    private LocalDateTime uploadTime; // 上传时间
    private String sessionId;       // 会话ID（关联聊天会话）
    
    // 构造函数
    public UploadedFile() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public UploadedFile(String originalName, String storedName, String filePath, 
                       String fileType, String mimeType, Long fileSize) {
        this();
        this.originalName = originalName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }
}