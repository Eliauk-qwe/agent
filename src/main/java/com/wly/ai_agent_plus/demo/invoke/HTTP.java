package com.wly.ai_agent_plus.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;

public class HTTP {

    public static String callQwenPlus(String apiKey) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 构建请求体
        String requestBody = JSONUtil.createObj()
                .set("model", "qwen-plus")
                .set("input", JSONUtil.createObj()
                        .set("messages", JSONUtil.createArray()
                                .put(JSONUtil.createObj()
                                        .set("role", "system")
                                        .set("content", "You are a helpful assistant."))
                                .put(JSONUtil.createObj()
                                        .set("role", "user")
                                        .set("content", "你是谁？"))))
                .set("parameters", JSONUtil.createObj()
                        .set("result_format", "message"))
                .toString();

        // 发送 POST 请求
        return HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .execute()
                .body();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String result = callQwenPlus(apiKey);
        System.out.println(result);
    }
}
