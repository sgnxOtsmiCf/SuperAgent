package cn.sgnxotsmicf.app.superagent;

import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Component
public class SuperAgent {

    private final StreamingHandler streamingHandler;

    private final SuperAgentFactory superAgentFactory;

    public SuperAgent(StreamingHandler streamingHandler, SuperAgentFactory superAgentFactory) {
        this.streamingHandler = streamingHandler;
        this.superAgentFactory = superAgentFactory;
    }


    public void doChatAgent(String message, String sessionId, SseEmitter emitter, Long userId, String userName) {
        doChatAgent(message, sessionId, emitter, userId, userName, null);
    }

    public void doChatAgent(String message, String sessionId, SseEmitter emitter, Long userId, String userName, String modelId) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
                .addMetadata("userId", userId)
                .addMetadata("userName", userName)
                .store(superAgentFactory.buildRedisStore())
                .build();
        try {
            ReactAgent reactAgent = superAgentFactory.createAgent(Map.of("userId", userId, "userName", userName), modelId);
            Flux<NodeOutput> stream = reactAgent.stream(message, config);
            streamingHandler.handle(stream, emitter);
        } catch (GraphRunnerException e) {
            log.error("Agent启动失败: {}", e.getMessage());
            streamingHandler.sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.AGENT_FAIL)));
        }
    }
}