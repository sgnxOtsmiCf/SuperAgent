package cn.sgnxotsmicf.agentTool.specialTool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class TerminateTool {
  
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task. \s
            "When you have finished all the tasks, call this tool to end the work. \s
           \s""")
    public String doTerminate() {  
        return "任务结束";  
    }  
}
