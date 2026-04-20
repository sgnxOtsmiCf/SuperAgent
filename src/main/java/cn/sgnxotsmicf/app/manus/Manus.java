package cn.sgnxotsmicf.app.manus;

import cn.sgnxotsmicf.app.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class Manus extends ToolCallAgent {

    public Manus(ToolCallback[] openManusTools) {
        super(openManusTools);
        this.setName("Manus");
        this.setSystemPrompt(Prompt.OpenManusSystemPrompt);
        this.setNextStepPrompt(Prompt.OpenManusNextSystemPrompt);
        this.setMaxSteps(20);
    }

}
