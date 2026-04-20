package com.wly.ai_agent_plus.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LoveAppTest {

    @Autowired
    private LoveApp loveApp;

    @Test
    void testSingleChat() {
        String response = loveApp.chat("我喜欢一个人，但不知道他是否喜欢我", "test-session-1");
        System.out.println("回复：" + response);
    }

    @Test
    void testMultiRoundChat() {
        String chatId = "test-session-2";

        // 第一轮：告诉模型一个具体的名字和事件
        String r1 = loveApp.chat("我喜欢一个叫小明的人，他昨天突然对我说'我们还是做朋友吧'", chatId);
        System.out.println("第一轮：" + r1);

        // 第二轮：补充细节，看模型是否记得"小明"
        String r2 = loveApp.chat("我们已经暧昧了两个月，我以为他也喜欢我", chatId);
        System.out.println("第二轮：" + r2);

        // 第三轮：直接问"他"，不提名字，验证模型是否还记得是小明
        String r3 = loveApp.chat("我喜欢的人是谁", chatId);
        System.out.println("第三轮：" + r3);
    }
}
