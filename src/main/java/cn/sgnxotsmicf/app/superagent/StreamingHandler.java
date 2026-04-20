package cn.sgnxotsmicf.app.superagent;

import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.SocketException;

@Slf4j
@Component
public class StreamingHandler {

    public void handle(Flux<NodeOutput> stream, SseEmitter emitter) {
        stream.subscribe(
                output -> processOutput(output, emitter),
                error -> {
                    // 忽略连接重置异常（前端断开连接导致）
                    if (isConnectionResetError(error)) {
                        log.error("检测到连接重置异常，可能是网络波动: {}", error.getMessage());
                        sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.MODEL_FAIL)));
                    } else {
                        error.printStackTrace();
                        log.error("流式调用异常:{}", error.getMessage());
                        sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.AGENT_FAIL)));
                    }
                },
                () -> completeEmitter(emitter)
        );
    }

    /**
     * 判断是否为连接重置错误
     */
    private boolean isConnectionResetError(Throwable error) {
        if (error == null) return false;

        // 检查异常消息
        String message = error.getMessage();
        if (message != null) {
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("connection reset") ||
                    lowerMsg.contains("connection reset by peer") ||
                    lowerMsg.contains("broken pipe") ||
                    lowerMsg.contains("unexpected end of stream") ||
                    lowerMsg.contains("connection closed")) {
                return true;
            }
        }

        // 检查异常类型
        if (error instanceof SocketException) {
            return true;
        }

        // 检查 cause
        return isConnectionResetError(error.getCause());
    }


    private void processOutput(NodeOutput output, SseEmitter emitter) {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }
        try {
            handleStreamingOutput(streamingOutput, emitter);
        } catch (IOException e) {
            sendError(emitter, JsonUtils.toJson(Result.build(ResultCodeEnum.AGENT_FAIL)));
            log.error("agent执行出错:{}", e.getMessage());
        }
    }

    private void handleStreamingOutput(StreamingOutput output, SseEmitter emitter)
            throws IOException {
        switch (output.getOutputType()) {
            case AGENT_MODEL_STREAMING -> {
                if (output.message() instanceof AssistantMessage assistantMessage) {
                    String reasoningContent = "";
                    if (output.message() instanceof DeepSeekAssistantMessage deepSeekAssistMessage) {
                        reasoningContent = deepSeekAssistMessage.getReasoningContent();
                    } else if (output.message() instanceof ZhiPuAiAssistantMessage zhiPuAiAssistantMessage) {
                        reasoningContent = zhiPuAiAssistantMessage.getReasoningContent();
                    } else {
                        Object reasoningContentObject = assistantMessage.getMetadata() != null ?
                                assistantMessage.getMetadata().get("reasoningContent") : null;
                        if (reasoningContentObject != null && !reasoningContentObject.toString().isEmpty()) {
                            reasoningContent = reasoningContentObject.toString();
                        }
                    }
                    if (reasoningContent != null && !reasoningContent.isEmpty()) {
                        //思考过程
                        log.info("reasoningContent:{}", reasoningContent);
                        emitter.send(SseEmitter.event()
                                .name("thinking")
                                .data(reasoningContent));
                    } else {
                        //普通响应
                        String text = output.message().getText();
                        if (text != null && !text.isEmpty()) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(text));
                        }
                    }

                }
            }
            case AGENT_MODEL_FINISHED -> {
                if (output.message() instanceof AssistantMessage assistantMessage) {
                    if (assistantMessage.hasToolCalls()) {
                        // 工具调用请求
                        emitter.send(SseEmitter.event()
                                .name("toolUsage")
                                .data(assistantMessage.getToolCalls()));
                    } else {
                        //模型完整响应
                        log.info("modelFinish");
//                        emitter.send(SseEmitter.event()
//                                .name("modelFinish")
//                                .data(output.message().getText()));
                    }
                }
            }
            case AGENT_TOOL_STREAMING -> emitter.send(SseEmitter.event()
                    .name("tool")
                    .data(output.message().getText()));
            case AGENT_TOOL_FINISHED -> {
                if (output.message() instanceof ToolResponseMessage toolResponse) {
                    emitter.send(SseEmitter
                            .event()
                            .name("toolResponse")
                            .data(toolResponse.getResponses()));
                }
                log.debug("toolResponse:{}", output.node());
            }
//            case AGENT_HOOK_STREAMING -> emitter.send(SseEmitter.event()
//                .name("hook")
//                .data(output.node()));
//            case AGENT_HOOK_FINISHED -> emitter.send(SseEmitter.event()
//                .name("hookFinish")
//                .data(output.node()));
        }
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("complete").data("Agent响应完成"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }
}