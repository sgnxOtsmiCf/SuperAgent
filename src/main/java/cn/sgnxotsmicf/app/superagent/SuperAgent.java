package cn.sgnxotsmicf.app.superagent;

import cn.sgnxotsmicf.app.superagent.factory.SuperAgentFactory;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Map;


/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 16:23
 * @Version: 0.1
 * @Description:
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAgent {

    private final StreamingHandler streamingHandler;

    private final SuperAgentFactory superAgentFactory;

    public void doChatAgent(ChatRequest request, SseEmitter emitter, Long userId, String userName) {
        String sessionId = request.getSessionId();
        String modelId = request.getModelId();
        String message = request.getMessage();
        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
                .addMetadata("userId", userId)
                .addMetadata("userName", userName)
                .store(superAgentFactory.buildRedisStore())
                .build();
        try {
            ReactAgent reactAgent = superAgentFactory.createAgent(Map.of("userId", userId, "userName", userName), request);
            Flux<NodeOutput> stream = reactAgent.stream(message, config);
            streamingHandler.handle(stream, emitter);
        } catch (GraphRunnerException e) {
            log.error("Agent启动失败: {}", e.getMessage());
            streamingHandler.sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.AGENT_FAIL)));
        }
    }

}