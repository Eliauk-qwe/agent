package com.wly.ai_agent_plus.Tool;

import cn.hutool.core.io.FileUtil;
import com.wly.ai_agent_plus.constant.File;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FileOperation {

    private  final  String FIVE_SAVE = File.FILE_SAVE_DIR+"/file";

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "name of the file to read") String filename){
        String filepath=FIVE_SAVE+"/"+filename;
        try {
            return FileUtil.readUtf8String(filepath);
        }catch (Exception e){
            return  "Error readfile:"+e.getMessage();
        }
    }


    @Tool(description = " write content to a file")
    public String writeFile(@ToolParam(description = "name of the file to write") String filename,@ToolParam(description = "content of the file to write") String content){
        String filepath=FIVE_SAVE+"/"+filename;

        try {
            FileUtil.writeUtf8String(filename,content);
            return "File "+filename+" successfully written to "+filepath;
        }catch (Exception e){
            return "Error writefile:"+e.getMessage();
        }
    }
}
