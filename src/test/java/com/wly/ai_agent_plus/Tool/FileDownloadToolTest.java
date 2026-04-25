package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件下载工具测试
 */
@SpringBootTest
class FileDownloadToolTest {

    @Autowired
    private FileDownloadTool fileDownloadTool;

    @Test
    void testBasicDownload() {
        System.out.println("\n========== 测试 1: 基础下载 ==========\n");
        
        // 下载一个小文件（示例：下载 example.com 的 HTML）
        FileDownloadTool.Request request = new FileDownloadTool.Request(
            "https://example.com"
        );
        request.fileName = "example.html";
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        // 验证
        assertTrue(response.success, "下载应该成功");
        assertNotNull(response.savePath, "应该有保存路径");
        assertTrue(response.fileSize > 0, "文件大小应该大于 0");
        
        System.out.println(response.toFormattedString());
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }

    @Test
    void testDownloadToSpecificDirectory() {
        System.out.println("\n========== 测试 2: 下载到指定目录 ==========\n");
        
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toDirectory(
            "https://example.com",
            "./test-downloads"
        );
        request.fileName = "test-example.html";
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertTrue(response.success);
        assertTrue(response.savePath.contains("test-downloads"));
        
        System.out.println(response.toFormattedString());
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }

    @Test
    void testDownloadWithCustomFileName() {
        System.out.println("\n========== 测试 3: 自定义文件名 ==========\n");
        
        FileDownloadTool.Request request = FileDownloadTool.Downloads.withFileName(
            "https://example.com",
            "tmp/download",
            "my-custom-name.html"
        );
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertTrue(response.success);
        assertEquals("my-custom-name.html", response.fileName);
        
        System.out.println(response.toFormattedString());
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }

    @Test
    void testDownloadToFullPath() {
        System.out.println("\n========== 测试 4: 完整路径下载 ==========\n");
        
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toPath(
            "https://example.com",
            "tmp/download/full-path-test.html"
        );
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertTrue(response.success);
        assertEquals("tmp/download/full-path-test.html", response.savePath);
        
        System.out.println(response.toFormattedString());
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }

    @Test
    void testFileExists() {
        System.out.println("\n========== 测试 5: 检查文件存在 ==========\n");
        
        String testPath = "tmp/download/exist-test.html";
        
        // 下载文件
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toPath(
            "https://example.com",
            testPath
        );
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertTrue(response.success);
        
        // 调试：打印路径信息
        System.out.println("请求路径: " + testPath);
        System.out.println("响应路径: " + response.savePath);
        
        // 检查文件是否存在 - 使用响应中的实际路径
        boolean exists = fileDownloadTool.fileExists(response.savePath);
        assertTrue(exists, "文件应该存在");
        
        System.out.println("文件存在: " + exists);
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
        
        // 再次检查
        exists = fileDownloadTool.fileExists(response.savePath);
        assertFalse(exists, "文件应该已被删除");
        
        System.out.println("删除后文件存在: " + exists);
    }

    @Test
    void testGetFileInfo() {
        System.out.println("\n========== 测试 6: 获取文件信息 ==========\n");
        
        String testPath = "tmp/download/info-test.html";
        
        // 下载文件
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toPath(
            "https://example.com",
            testPath
        );
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        // 调试：打印响应信息
        System.out.println("下载成功: " + response.success);
        System.out.println("保存路径: " + response.savePath);
        System.out.println("文件存在检查: " + fileDownloadTool.fileExists(response.savePath));
        
        // 获取文件信息 - 使用响应中的实际路径
        FileDownloadTool.FileInfo info = fileDownloadTool.getFileInfo(response.savePath);
        
        assertNotNull(info, "应该能获取文件信息");
        assertEquals("info-test.html", info.name);
        assertTrue(info.size > 0);
        assertTrue(info.canRead);
        
        System.out.println(info);
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }

    @Test
    void testBatchDownload() {
        System.out.println("\n========== 测试 7: 批量下载 ==========\n");
        
        FileDownloadTool.Request[] requests = {
            FileDownloadTool.Downloads.withFileName(
                "https://example.com", 
                "tmp/download", 
                "batch-1.html"
            ),
            FileDownloadTool.Downloads.withFileName(
                "https://example.com", 
                "tmp/download", 
                "batch-2.html"
            ),
            FileDownloadTool.Downloads.withFileName(
                "https://example.com", 
                "tmp/download", 
                "batch-3.html"
            )
        };
        
        FileDownloadTool.Response[] responses = fileDownloadTool.downloadBatch(requests);
        
        assertEquals(3, responses.length);
        
        for (int i = 0; i < responses.length; i++) {
            System.out.println("\n文件 " + (i + 1) + ":");
            System.out.println(responses[i].toFormattedString());
            assertTrue(responses[i].success);
            
            // 清理
            fileDownloadTool.deleteFile(responses[i].savePath);
        }
    }

    @Test
    void testInvalidUrl() {
        System.out.println("\n========== 测试 8: 无效 URL ==========\n");
        
        FileDownloadTool.Request request = new FileDownloadTool.Request(
            "https://this-url-does-not-exist-12345.com/file.txt"
        );
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertFalse(response.success, "无效 URL 应该失败");
        assertNotNull(response.error, "应该有错误信息");
        
        System.out.println("错误信息: " + response.error);
    }

    @Test
    void testDownloadSpeed() {
        System.out.println("\n========== 测试 9: 下载速度计算 ==========\n");
        
        FileDownloadTool.Request request = new FileDownloadTool.Request(
            "https://example.com"
        );
        request.fileName = "speed-test.html";
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        if (response.success) {
            System.out.println("文件大小: " + response.fileSize + " bytes");
            System.out.println("下载时间: " + response.downloadTimeMs + " ms");
            System.out.println("下载速度: " + String.format("%.2f KB/s", response.speedKBps));
            
            assertTrue(response.speedKBps > 0, "下载速度应该大于 0");
            
            // 清理
            fileDownloadTool.deleteFile(response.savePath);
        }
    }

    @Test
    void testAutoCreateDirectory() {
        System.out.println("\n========== 测试 10: 自动创建目录 ==========\n");
        
        // 下载到一个不存在的目录
        FileDownloadTool.Request request = FileDownloadTool.Downloads.toDirectory(
            "https://example.com",
            "tmp/download/nested/deep/directory"
        );
        request.fileName = "auto-dir-test.html";
        
        FileDownloadTool.Response response = fileDownloadTool.apply(request);
        
        assertTrue(response.success, "应该自动创建目录并下载成功");
        
        // 使用响应中的实际路径检查
        assertTrue(fileDownloadTool.fileExists(response.savePath), "文件应该存在");
        
        System.out.println(response.toFormattedString());
        
        // 清理
        fileDownloadTool.deleteFile(response.savePath);
    }
}
