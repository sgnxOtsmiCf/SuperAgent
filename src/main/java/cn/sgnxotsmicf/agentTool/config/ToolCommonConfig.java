package cn.sgnxotsmicf.agentTool.config;

import cn.sgnxotsmicf.agentTool.commonTool.AskUserQuestionTool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/9 16:52
 * @Version: 1.0
 * @Description:
 */
@Component
public class ToolCommonConfig {

    @Bean
    public AskUserQuestionTool askUserQuestionTool() {
        return AskUserQuestionTool
                .builder()
                .questionHandler((userId, questions) -> {
                    Map<String, String> answers = new HashMap<>();
                    for (AskUserQuestionTool.Question q : questions) {
                        answers.put(q.question(), "用户" + userId + "的回答");
                    }
                    return answers;
                }).build();
    }

}
