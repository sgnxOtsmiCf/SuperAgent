package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ToolVo;
import cn.sgnxotsmicf.service.FunctionService;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 18:35
 * @Version: 1.0
 * @Description:
 */

@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final ToolRegistry toolRegistry;

    private final HookRegistry hookRegistry;

    @Override
    public Result<List<ToolVo>> getFunction() {
        ArrayList<ToolVo> toolVos = new ArrayList<>();
        for (ToolCallback toolCallback : toolRegistry.openManusTools()) {
            String name = toolCallback.getToolDefinition().name();
            String value = toolCallback.getToolDefinition().description();
            ToolVo toolVo = ToolVo.builder().name(name).description(value).build();
            toolVos.add(toolVo);
        }
        return Result.ok(toolVos);
    }

    @Override
    public Result<List<ToolVo>> getSkills() {
        SkillsAgentHook skillsAgentHook = hookRegistry.buildFileSystemSkillsAgentHook();
        List<SkillMetadata> skillMetadata = skillsAgentHook.listSkills();
        ArrayList<ToolVo> toolVos = new ArrayList<>();
        for (SkillMetadata skill : skillMetadata) {
            String name = skill.getName();
            String value = skill.getDescription();
            ToolVo toolVo = ToolVo.builder().name(name).description(value).build();
            toolVos.add(toolVo);
        }
        return Result.ok(toolVos);
    }
}
