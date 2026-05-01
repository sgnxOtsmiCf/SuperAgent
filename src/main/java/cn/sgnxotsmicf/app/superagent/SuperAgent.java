package cn.sgnxotsmicf.app.superagent;

import cn.sgnxotsmicf.app.superagent.factory.SuperAgentFactory;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAgent {

    private final StreamingHandler streamingHandler;

    private final SuperAgentFactory superAgentFactory;

    public void doChatAgent(ChatRequest request, SseEmitter emitter, Long userId, String userName) {
        String sessionId = request.getSessionId();
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

            StringBuffer reasoningAccumulator = new StringBuffer();
            Flux<NodeOutput> enrichedStream = stream
                    .doOnNext(output -> accumulateReasoningContent(output, reasoningAccumulator))
                    .doOnComplete(() -> persistReasoningContent(sessionId, config, reasoningAccumulator.toString()));

            streamingHandler.handle(enrichedStream, emitter);
        } catch (GraphRunnerException e) {
            log.error("Agent启动失败: {}", e.getMessage());
            streamingHandler.sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.AGENT_FAIL)));
        }
    }

    private void accumulateReasoningContent(NodeOutput output, StringBuffer accumulator) {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }
        if (streamingOutput.getOutputType() != OutputType.AGENT_MODEL_STREAMING) {
            return;
        }
        if (!(streamingOutput.message() instanceof AssistantMessage assistantMessage)) {
            return;
        }
        String reasoningContent = extractReasoningContent(assistantMessage);
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            accumulator.append(reasoningContent);
        }
    }

    private String extractReasoningContent(AssistantMessage assistantMessage) {
        if (assistantMessage instanceof DeepSeekAssistantMessage deepSeekAssistMessage) {
            return deepSeekAssistMessage.getReasoningContent();
        } else if (assistantMessage instanceof ZhiPuAiAssistantMessage zhiPuAiAssistantMessage) {
            return zhiPuAiAssistantMessage.getReasoningContent();
        } else {
            Object reasoningContentObject = assistantMessage.getMetadata() != null ?
                    assistantMessage.getMetadata().get("reasoningContent") : null;
            if (reasoningContentObject != null && !reasoningContentObject.toString().isEmpty()) {
                return reasoningContentObject.toString();
            }
        }
        return null;
    }

    private void persistReasoningContent(String sessionId, RunnableConfig config, String reasoningContent) {
        if (reasoningContent == null || reasoningContent.isEmpty()) {
            return;
        }
        try {
            RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
            Optional<Checkpoint> optionalCheckpoint = redisSaver.get(config);
            if (optionalCheckpoint.isEmpty()) {
                return;
            }
            Checkpoint checkpoint = optionalCheckpoint.get();
            Object messagesObj = checkpoint.getState().get("messages");
            if (!(messagesObj instanceof ArrayList<?> messageList)) {
                return;
            }
            int lastUserMsgIndex = -1;
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (messageList.get(i) instanceof UserMessage) {
                    lastUserMsgIndex = i;
                    break;
                }
            }
            if (lastUserMsgIndex < 0) {
                return;
            }
            for (int i = lastUserMsgIndex + 1; i < messageList.size(); i++) {
                if (messageList.get(i) instanceof AssistantMessage assistantMessage) {
                    Map<String, Object> newMetadata = new HashMap<>(assistantMessage.getMetadata());
                    newMetadata.put("reasoningContent", reasoningContent);
                    AssistantMessage updated = AssistantMessage.builder()
                            .content(assistantMessage.getText())
                            .toolCalls(assistantMessage.getToolCalls())
                            .properties(newMetadata)
                            .build();
                    ((ArrayList<Message>) messageList).set(i, updated);
                    break;
                }
            }
            redisSaver.put(config, checkpoint);
            log.debug("推理内容已持久化到checkpoint, sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("持久化推理内容失败, sessionId={}: {}", sessionId, e.getMessage());
        }
    }

}