package com.wly.ai_agent_plus.Tool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FileOperation 集成测试
 */
@SpringBootTest
public class FileOperationIntegrationTest {

    @Test
    public void testReadDownloadedFile() {
        FileOperation fileOperation = new FileOperation();
        
        // 测试读取下载目录中的文件
        String result1 = fileOperation.readFile("tmp/download/YuManusTest.java");
        System.out.println("读取 YuManusTest.java 结果:");
        System.out.println(result1);
        System.out.println("---");
        
        // 测试读取测试文件
        String result2 = fileOperation.readFile("tmp/download/test-file.txt");
        System.out.println("读取 test-file.txt 结果:");
        System.out.println(result2);
        System.out.println("---");
        
        // 测试不存在的文件
        String result3 = fileOperation.readFile("non-existent-file.txt");
        System.out.println("读取不存在文件的结果:");
        System.out.println(result3);
    }
}