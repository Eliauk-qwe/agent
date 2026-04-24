package com.wly.ai_agent_plus.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class LoveAppTest {

    @Autowired
    private LoveApp loveApp;

    @Test
    void testSingleChat() {
        String chatId = UUID.randomUUID().toString();
        loveApp.chat("我喜欢一个人，但不知道他是否喜欢我", chatId);
        //System.out.println("回复：" + response);
    }

    @Test
    void testMultiRoundChat() {
        String chatId = UUID.randomUUID().toString();


        // 第一轮：告诉模型一个具体的名字和事件
        loveApp.chat("我喜欢一个叫小明的人，他昨天突然对我说'我们还是做朋友吧'", chatId);
        //System.out.println("第一轮：" + r1);

        // 第二轮：补充细节，看模型是否记得"小明"
        loveApp.chat("我们已经暧昧了两个月，赌博，我以为他也喜欢我", chatId);
        //System.out.println("第二轮：" + r2);

        // 第三轮：直接问"他"，不提名字，验证模型是否还记得是小明
        loveApp.chat("我喜欢的人是谁", chatId);
        //System.out.println("第三轮：" + r3);
    }


    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我想让另一半更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.dowithreport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        loveApp.chatwithrag(message, chatId);
    }


}
