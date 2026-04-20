package cn.sgnxotsmicf.app.family;

import cn.sgnxotsmicf.app.Prompt;
import cn.sgnxotsmicf.app.manus.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class FamilyHarmony extends ToolCallAgent {

    public FamilyHarmony(ToolCallback[] openManusTools) {
        super(openManusTools);
        this.setName("家庭和睦ai智能体");
        this.setSystemPrompt(Prompt.FamilyHarmonySystemPrompt);
        this.setNextStepPrompt(Prompt.FamilyHarmonyNextSystemPrompt);
        this.setMaxSteps(10);
    }

}
