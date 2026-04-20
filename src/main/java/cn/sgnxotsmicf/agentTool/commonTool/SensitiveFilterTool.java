package cn.sgnxotsmicf.agentTool.commonTool;

import com.alibaba.cloud.ai.toolcalling.sensitivefilter.SensitiveFilterService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/27 15:18
 * @Version: 1.0
 * @Description:
 */

@Component
public class SensitiveFilterTool {

    @Resource
    private SensitiveFilterService sensitiveFilter;

    /**
     * 核心：注册为 Function Bean → Spring AI 自动转换为 ToolCallback
     * @Tool 注解：定义工具名称+描述，AI模型会识别并调用
     */
    @Tool(description = "Filter sensitive information such as phone numbers, ID numbers and bank card numbers in the text.")
    public String sensitiveFilter(@ToolParam(description = "The input text string parameter.") String context) {
        return sensitiveFilter.apply(context);
    }

}
