package com.wly.ai_agent_plus.Tool;

import com.wly.ai_agent_plus.entity.UploadedFile;
import com.wly.ai_agent_plus.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件分析工具 - 让AI可以分析用户上传的文件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileAnalysisTool {
    
    private final FileUploadService fileUploadService;
    
    /**
     * 获取指定文件的内容进行分析
     */
    @Tool(name = "analyzeFile", description = "分析用户上传的文件内容，获取文件的文本内容用于回答问题")
    public String analyzeFile(@ToolParam(description = "要分析的文件ID") String fileId) {
        try {
            UploadedFile file = fileUploadService.getFile(fileId);
            
            if (file == null) {
                return "错误：找不到指定的文件 ID: " + fileId;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("文件信息：\n");
            result.append("- 文件名：").append(file.getOriginalName()).append("\n");
            result.append("- 文件类型：").append(file.getFileType()).append("\n");
            result.append("- 文件大小：").append(formatFileSize(file.getFileSize())).append("\n");
            result.append("- 上传时间：").append(file.getUploadTime()).append("\n\n");
            
            if (file.getExtractedText() != null && !file.getExtractedText().trim().isEmpty()) {
                result.append("文件内容：\n");
                result.append(file.getExtractedText());
            } else {
                result.append("该文件无法提取文本内容或为空文件。");
                if ("image".equals(file.getFileType())) {
                    result.append("这是一个图片文件，您可以通过 /api/files/").append(fileId).append("/download 查看图片。");
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("分析文件失败: {}", e.getMessage(), e);
            return "分析文件时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 获取会话中所有上传的文件列表
     */
    @Tool(name = "listUploadedFiles", description = "获取当前会话中用户上传的所有文件列表")
    public String listUploadedFiles(@ToolParam(description = "会话ID") String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "default";
            }
            
            List<UploadedFile> files = fileUploadService.getFilesBySession(sessionId);
            
            if (files.isEmpty()) {
                return "当前会话中没有上传的文件。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("当前会话中的上传文件列表：\n\n");
            
            for (int i = 0; i < files.size(); i++) {
                UploadedFile file = files.get(i);
                result.append(i + 1).append(". ");
                result.append("文件名：").append(file.getOriginalName()).append("\n");
                result.append("   文件ID：").append(file.getId()).append("\n");
                result.append("   类型：").append(file.getFileType()).append("\n");
                result.append("   大小：").append(formatFileSize(file.getFileSize())).append("\n");
                result.append("   上传时间：").append(file.getUploadTime()).append("\n");
                result.append("   可提取文本：").append(file.getExtractedText() != null && !file.getExtractedText().trim().isEmpty() ? "是" : "否").append("\n\n");
            }
            
            result.append("您可以使用 analyzeFile 工具分析具体文件的内容。");
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return "获取文件列表时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 搜索文件内容
     */
    @Tool(name = "searchInFiles", description = "在用户上传的文件中搜索指定的关键词")
    public String searchInFiles(@ToolParam(description = "会话ID") String sessionId, 
                               @ToolParam(description = "要搜索的关键词") String keyword) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "default";
            }
            
            if (keyword == null || keyword.trim().isEmpty()) {
                return "请提供要搜索的关键词。";
            }
            
            List<UploadedFile> files = fileUploadService.getFilesBySession(sessionId);
            
            if (files.isEmpty()) {
                return "当前会话中没有上传的文件。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("搜索关键词 \"").append(keyword).append("\" 的结果：\n\n");
            
            boolean found = false;
            
            for (UploadedFile file : files) {
                if (file.getExtractedText() != null && 
                    file.getExtractedText().toLowerCase().contains(keyword.toLowerCase())) {
                    
                    found = true;
                    result.append("在文件 \"").append(file.getOriginalName()).append("\" 中找到匹配内容：\n");
                    result.append("文件ID：").append(file.getId()).append("\n");
                    
                    // 提取包含关键词的上下文
                    String[] lines = file.getExtractedText().split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].toLowerCase().contains(keyword.toLowerCase())) {
                            result.append("第 ").append(i + 1).append(" 行：").append(lines[i].trim()).append("\n");
                        }
                    }
                    result.append("\n");
                }
            }
            
            if (!found) {
                result.append("在已上传的文件中未找到包含关键词 \"").append(keyword).append("\" 的内容。");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("搜索文件内容失败: {}", e.getMessage(), e);
            return "搜索文件内容时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}