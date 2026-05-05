package com.wly.ai_agent_plus.Tool;

import cn.hutool.core.io.FileUtil;
import com.wly.ai_agent_plus.constant.File;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileOperation {

    private final String FILE_SAVE_DIR = File.FILE_SAVE_DIR + "/file";

    @Tool(description = "Read content from a file. Supports both relative paths (from project root) and absolute paths.")
    public String readFile(@ToolParam(description = "path of the file to read (relative to project root or absolute path)") String filepath) {
        try {
            Path path = resolvePath(filepath);
            
            if (!Files.exists(path)) {
                return "Error readfile:File not exist: " + path.toAbsolutePath();
            }
            
            if (!Files.isReadable(path)) {
                return "Error readfile:File not readable: " + path.toAbsolutePath();
            }
            
            return FileUtil.readUtf8String(path.toFile());
        } catch (Exception e) {
            return "Error readfile:" + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file. Creates directories if they don't exist.")
    public String writeFile(@ToolParam(description = "path of the file to write (relative to project root or absolute path)") String filepath, 
                           @ToolParam(description = "content of the file to write") String content) {
        try {
            Path path = resolvePath(filepath);
            
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            FileUtil.writeUtf8String(content, path.toFile());
            return "File successfully written to " + path.toAbsolutePath();
        } catch (Exception e) {
            return "Error writefile:" + e.getMessage();
        }
    }
    
    /**
     * 解析文件路径，支持相对路径和绝对路径
     */
    private Path resolvePath(String filepath) {
        Path path = Paths.get(filepath);
        
        // 如果是绝对路径，直接返回
        if (path.isAbsolute()) {
            return path;
        }
        
        // 如果是相对路径，先尝试相对于项目根目录
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path resolvedPath = projectRoot.resolve(filepath);
        
        // 如果文件存在，返回这个路径
        if (Files.exists(resolvedPath)) {
            return resolvedPath;
        }
        
        // 如果文件不存在，尝试相对于默认文件目录
        Path defaultPath = Paths.get(FILE_SAVE_DIR).resolve(filepath);
        return defaultPath;
    }
}
