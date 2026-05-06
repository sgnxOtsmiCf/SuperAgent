package cn.sgnxotsmicf.app.superagent.hook;

import cn.sgnxotsmicf.app.superagent.hook.agent.TotalTokenSummaryHook;
import cn.sgnxotsmicf.app.superagent.hook.log.AgentHookLog;
import cn.sgnxotsmicf.app.superagent.hook.log.ModelHookLog;
import cn.sgnxotsmicf.app.superagent.hook.message.ValidateResponseHook;
import cn.sgnxotsmicf.app.superagent.hook.model.MemoryHook;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HookRegistry {

    private final AgentHookLog agentHookLog;

    private final ModelHookLog modelHookLog;

    private final ChatModel dashscopeChatModel;

    private final MemoryHook memoryHook;

    private final TotalTokenSummaryHook totalTokenSummaryHook;

    public List<Hook> buildHooks() {
        return Arrays.asList(
                //totalTokenSummaryHook,
                new ValidateResponseHook(),
                //agentHookLog,
                //modelHookLog,
                buildModelCallLimitHook(),
                buildSummarizationHook(),
                buildFileSystemSkillsAgentHook(),
                memoryHook
        );
    }

    private ModelCallLimitHook buildModelCallLimitHook() {
        return ModelCallLimitHook.builder()
                .runLimit(10)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();
    }

    private SummarizationHook buildSummarizationHook() {
        return SummarizationHook.builder()
                .model(dashscopeChatModel)
                .maxTokensBeforeSummary(400000)
                .messagesToKeep(20)
                .keepFirstUserMessage(true)
                .build();
    }

    public SkillsAgentHook buildFileSystemSkillsAgentHook() {
        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(System.getProperty("user.dir") + "/skills/SuperAgent")
                .build();
        return SkillsAgentHook.builder()
                .skillRegistry(registry)
                .autoReload(false)
                .build();
    }

    private SkillsAgentHook buildClasspathSkillAgentHook() {
        SkillRegistry registry = ClasspathSkillRegistry.builder()
                .classpathPath("skills")//可选 .basePath("/tmp") 指定 JAR 内资源复制到的目录（默认 /tmp）。
                .build();
        return SkillsAgentHook.builder()
                .skillRegistry(registry)
                .build();
    }

}