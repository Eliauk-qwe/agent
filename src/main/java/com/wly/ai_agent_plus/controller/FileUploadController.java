package com.wly.ai_agent_plus.controller;

import com.wly.ai_agent_plus.entity.UploadedFile;
import com.wly.ai_agent_plus.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件上传", description = "文件上传和管理API")
public class FileUploadController {
    
    private final FileUploadService fileUploadService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "支持图片、PDF、Word、文本等文件类型")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "会话ID") @RequestParam(value = "sessionId", required = false, defaultValue = "default") String sessionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UploadedFile uploadedFile = fileUploadService.uploadFile(file, sessionId);
            
            response.put("success", true);
            response.put("message", "文件上传成功");
            response.put("data", Map.of(
                "fileId", uploadedFile.getId().toString(),
                "originalName", uploadedFile.getOriginalName(),
                "fileType", uploadedFile.getFileType(),
                "fileSize", uploadedFile.getFileSize(),
                "uploadTime", uploadedFile.getUploadTime(),
                "hasText", uploadedFile.getExtractedText() != null && !uploadedFile.getExtractedText().trim().isEmpty()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}/info")
    @Operation(summary = "获取文件信息")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String fileId) {
        UploadedFile file = fileUploadService.getFile(fileId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (file == null) {
            response.put("success", false);
            response.put("message", "文件不存在");
            return ResponseEntity.notFound().build();
        }
        
        response.put("success", true);
        response.put("data", Map.of(
            "fileId", file.getId().toString(),
            "originalName", file.getOriginalName(),
            "fileType", file.getFileType(),
            "mimeType", file.getMimeType(),
            "fileSize", file.getFileSize(),
            "uploadTime", file.getUploadTime(),
            "extractedText", file.getExtractedText() != null ? file.getExtractedText() : "",
            "hasText", file.getExtractedText() != null && !file.getExtractedText().trim().isEmpty()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 下载/查看文件
     */
    @GetMapping("/{fileId}/download")
    @Operation(summary = "下载或查看文件")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        UploadedFile file = fileUploadService.getFile(fileId);
        
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Resource resource = new FileSystemResource(file.getFilePath());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定Content-Type
            String contentType = file.getMimeType();
            if (contentType == null) {
                contentType = Files.probeContentType(Paths.get(file.getFilePath()));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename=\"" + file.getOriginalName() + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("文件下载失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取会话的所有文件
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话的所有文件")
    public ResponseEntity<Map<String, Object>> getSessionFiles(@PathVariable String sessionId) {
        List<UploadedFile> files = fileUploadService.getFilesBySession(sessionId);
        
        List<Map<String, Object>> fileList = files.stream()
            .map(file -> {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("fileId", file.getId().toString());
                fileMap.put("originalName", file.getOriginalName());
                fileMap.put("fileType", file.getFileType());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("uploadTime", file.getUploadTime());
                fileMap.put("hasText", file.getExtractedText() != null && !file.getExtractedText().trim().isEmpty());
                return fileMap;
            })
            .toList();
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", fileList
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId) {
        boolean deleted = fileUploadService.deleteFile(fileId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (deleted) {
            response.put("success", true);
            response.put("message", "文件删除成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "文件删除失败或文件不存在");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取文件的文本内容（供AI分析使用）
     */
    @GetMapping("/{fileId}/text")
    @Operation(summary = "获取文件的文本内容")
    public ResponseEntity<Map<String, Object>> getFileText(@PathVariable String fileId) {
        UploadedFile file = fileUploadService.getFile(fileId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (file == null) {
            response.put("success", false);
            response.put("message", "文件不存在");
            return ResponseEntity.notFound().build();
        }
        
        response.put("success", true);
        response.put("data", Map.of(
            "fileId", file.getId().toString(),
            "originalName", file.getOriginalName(),
            "fileType", file.getFileType(),
            "extractedText", file.getExtractedText() != null ? file.getExtractedText() : "",
            "hasText", file.getExtractedText() != null && !file.getExtractedText().trim().isEmpty()
        ));
        
        return ResponseEntity.ok(response);
    }
}