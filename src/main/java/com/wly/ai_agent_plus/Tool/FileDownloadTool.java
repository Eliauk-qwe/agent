package com.wly.ai_agent_plus.Tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Function;

/**
 * 文件下载工具
 * 
 * 使用 Hutool 的 HttpUtil.downloadFile 实现文件下载
 * 
 * 功能：
 *   - 下载网络文件到本地
 *   - 支持自定义保存路径
 *   - 自动创建目录
 *   - 获取文件大小
 *   - 下载进度跟踪
 */
@Component
@Slf4j
public class FileDownloadTool implements Function<FileDownloadTool.Request, FileDownloadTool.Response> {

    // 默认下载目录
    private static final String DEFAULT_DOWNLOAD_DIR = "tmp/download";
    
    // 默认超时时间（毫秒）
    private static final int DEFAULT_TIMEOUT = 60000; // 60 秒

    /**
     * 下载文件
     * 
     * @param request 下载请求
     * @return 下载结果
     */
    @Tool(name = "downloadFile", description = "Download files from URLs to tmp/download directory. Supports custom file names and paths.")
    @Override
    public Response apply(@ToolParam(description = "Download request with URL and optional file name") Request request) {
        log.info("开始下载文件: {}", request.url);
        
        Response response = new Response();
        response.url = request.url;
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 确定保存路径
            String savePath = determineSavePath(request);
            response.savePath = savePath;
            
            // 确保目录存在
            File saveFile = new File(savePath);
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                log.debug("创建目录: {}, 结果: {}", parentDir.getAbsolutePath(), created);
            }
            
            // 下载文件
            log.info("下载到: {}", savePath);
            long size = HttpUtil.downloadFile(request.url, saveFile);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 设置响应信息
            response.success = true;
            response.fileSize = size;
            response.downloadTimeMs = duration;
            response.fileName = saveFile.getName();
            
            // 计算下载速度（KB/s）
            if (duration > 0) {
                response.speedKBps = (size / 1024.0) / (duration / 1000.0);
            }
            
            log.info("下载完成，文件大小: {} bytes, 耗时: {}ms, 速度: {:.2f} KB/s", 
                    size, duration, response.speedKBps);
            
        } catch (Exception e) {
            log.error("下载失败: {}", e.getMessage(), e);
            response.success = false;
            response.error = "下载失败: " + e.getMessage();
        }
        
        return response;
    }

    /**
     * 确定保存路径
     */
    private String determineSavePath(Request request) {
        // 如果指定了完整路径，直接使用
        if (request.savePath != null && !request.savePath.isEmpty()) {
            return request.savePath;
        }
        
        // 如果只指定了目录，从 URL 中提取文件名
        String directory = request.saveDirectory != null && !request.saveDirectory.isEmpty() 
            ? request.saveDirectory 
            : DEFAULT_DOWNLOAD_DIR;
        
        String fileName = request.fileName != null && !request.fileName.isEmpty()
            ? request.fileName
            : extractFileNameFromUrl(request.url);
        
        return directory + File.separator + fileName;
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            // 移除查询参数
            String urlWithoutParams = url.split("\\?")[0];
            
            // 获取最后一个 / 后面的内容
            String[] parts = urlWithoutParams.split("/");
            String fileName = parts[parts.length - 1];
            
            // 如果文件名为空或太短，使用时间戳
            if (fileName.isEmpty() || fileName.length() < 3) {
                fileName = "download_" + System.currentTimeMillis();
            }
            
            return fileName;
        } catch (Exception e) {
            return "download_" + System.currentTimeMillis();
        }
    }

    /**
     * 下载请求
     */
    public static class Request {
        /**
         * 下载 URL（必填）
         */
        public String url;
        
        /**
         * 完整保存路径（可选，如果指定则忽略 saveDirectory 和 fileName）
         */
        public String savePath;
        
        /**
         * 保存目录（可选，默认 ./downloads）
         */
        public String saveDirectory;
        
        /**
         * 文件名（可选，默认从 URL 提取）
         */
        public String fileName;
        
        /**
         * 超时时间（毫秒，可选）
         */
        public int timeout;

        public Request() {}

        public Request(String url) {
            this.url = url;
        }

        public Request(String url, String savePath) {
            this.url = url;
            this.savePath = savePath;
        }

        public Request(String url, String saveDirectory, String fileName) {
            this.url = url;
            this.saveDirectory = saveDirectory;
            this.fileName = fileName;
        }
    }

    /**
     * 下载响应
     */
    public static class Response {
        /**
         * 原始 URL
         */
        public String url;
        
        /**
         * 是否成功
         */
        public boolean success;
        
        /**
         * 保存路径
         */
        public String savePath;
        
        /**
         * 文件名
         */
        public String fileName;
        
        /**
         * 文件大小（字节）
         */
        public long fileSize;
        
        /**
         * 下载时间（毫秒）
         */
        public long downloadTimeMs;
        
        /**
         * 下载速度（KB/s）
         */
        public double speedKBps;
        
        /**
         * 错误信息
         */
        public String error;

        /**
         * 格式化输出
         */
        public String toFormattedString() {
            if (!success) {
                return "下载失败: " + error;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("【下载成功】\n");
            sb.append("文件名: ").append(fileName).append("\n");
            sb.append("保存路径: ").append(savePath).append("\n");
            sb.append("文件大小: ").append(formatFileSize(fileSize)).append("\n");
            sb.append("下载时间: ").append(downloadTimeMs).append("ms\n");
            sb.append("下载速度: ").append(String.format("%.2f KB/s", speedKBps)).append("\n");
            
            return sb.toString();
        }

        /**
         * 格式化文件大小
         */
        private String formatFileSize(long size) {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.2f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", size / (1024.0 * 1024));
            } else {
                return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
            }
        }
    }

    /**
     * 常用下载快捷方法
     */
    public static class Downloads {
        /**
         * 下载到默认目录
         */
        public static Request toDefaultDir(String url) {
            return new Request(url);
        }

        /**
         * 下载到指定目录
         */
        public static Request toDirectory(String url, String directory) {
            Request request = new Request(url);
            request.saveDirectory = directory;
            return request;
        }

        /**
         * 下载并指定文件名
         */
        public static Request withFileName(String url, String directory, String fileName) {
            return new Request(url, directory, fileName);
        }

        /**
         * 下载到完整路径
         */
        public static Request toPath(String url, String fullPath) {
            return new Request(url, fullPath);
        }
    }

    /**
     * 批量下载
     */
    public Response[] downloadBatch(Request[] requests) {
        Response[] responses = new Response[requests.length];
        
        for (int i = 0; i < requests.length; i++) {
            log.info("批量下载 {}/{}", i + 1, requests.length);
            responses[i] = apply(requests[i]);
            
            // 添加延迟，避免请求过快
            if (i < requests.length - 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return responses;
    }

    /**
     * 检查文件是否已存在
     */
    public boolean fileExists(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    /**
     * 删除已下载的文件
     */
    public boolean deleteFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取文件信息
     */
    public FileInfo getFileInfo(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        FileInfo info = new FileInfo();
        info.path = path;
        info.name = file.getName();
        info.size = file.length();
        info.lastModified = file.lastModified();
        info.canRead = file.canRead();
        info.canWrite = file.canWrite();
        
        return info;
    }

    /**
     * 文件信息
     */
    public static class FileInfo {
        public String path;
        public String name;
        public long size;
        public long lastModified;
        public boolean canRead;
        public boolean canWrite;

        @Override
        public String toString() {
            return String.format("文件: %s, 大小: %d bytes, 最后修改: %tc", 
                    name, size, lastModified);
        }
    }
}
