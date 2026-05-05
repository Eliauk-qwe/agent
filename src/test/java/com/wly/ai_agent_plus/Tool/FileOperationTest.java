package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileOperation 工具测试
 */
public class FileOperationTest {

    private FileOperation fileOperation;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileOperation = new FileOperation();
    }

    @Test
    void testReadFileWithRelativePath() throws IOException {
        // 创建测试文件
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());

        // 测试读取（使用绝对路径，因为相对路径需要在项目根目录下）
        String result = fileOperation.readFile(testFile.toString());
        assertEquals(content, result);
    }

    @Test
    void testReadNonExistentFile() {
        String result = fileOperation.readFile("non-existent-file.txt");
        assertTrue(result.startsWith("Error readfile:File not exist:"));
    }

    @Test
    void testWriteFile() throws IOException {
        Path testFile = tempDir.resolve("write-test.txt");
        String content = "Test content";

        String result = fileOperation.writeFile(testFile.toString(), content);
        assertTrue(result.contains("successfully written"));

        // 验证文件内容
        String readContent = Files.readString(testFile);
        assertEquals(content, readContent);
    }

    @Test
    void testWriteFileWithDirectoryCreation() {
        Path testFile = tempDir.resolve("subdir/test.txt");
        String content = "Test content";

        String result = fileOperation.writeFile(testFile.toString(), content);
        assertTrue(result.contains("successfully written"));

        // 验证文件存在
        assertTrue(Files.exists(testFile));
    }
}