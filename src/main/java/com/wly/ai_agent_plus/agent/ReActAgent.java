package com.wly.ai_agent_plus.agent;


import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent {

    public  abstract  boolean think();

    public  abstract  String  act();

    @Override
    public  String step(){
        try{
            boolean  shouldact=think();
            if(!shouldact){
                // 如果不需要行动，返回AI的最后回复
                if (!getMessageList().isEmpty()) {
                    Message lastMessage = getMessageList().get(getMessageList().size() - 1);
                    if (lastMessage instanceof AssistantMessage assistantMessage) {
                        String response = assistantMessage.getText();
                        if (StrUtil.isNotBlank(response)) {
                            return response;
                        }
                    }
                }
                return "完成思考，无需行动";
            }else{
                return act();
            }
        } catch (Exception e) {
            return "步骤执行失败"+e.getMessage();
        }
    }


}
