package cn.sgnxotsmicf.app.manus;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.app.manus.model.AgentContext;
import cn.sgnxotsmicf.app.manus.model.AgentState;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.exception.AgentException;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;

    private String nextStepPrompt;

    private int maxSteps = 10;

    public String run(AgentContext context, String userPrompt, ChatClient chatClient) {
        context.setChatClient(chatClient);
        if (context.getState() != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + context.getState());
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        context.setState(AgentState.RUNNING);
        context.getMessageList().add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && context.getState() != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                context.setCurrentStep(stepNumber);
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                String stepResult = step(context);
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            if (context.getCurrentStep() >= maxSteps) {
                context.setState(AgentState.FINISHED);
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            context.setState(AgentState.ERROR);
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            cleanup(context);
        }
    }

    public SseEmitter runStream(String userPrompt, String sessionId, SseEmitter emitter, ChatClient chatClient) {
        if ((sessionId == null || sessionId.isBlank())) {
            throw new AgentException(ResultCodeEnum.AGENT_SESSION_EMPTY);
        }
        AgentContext context = new AgentContext(sessionId);
        context.setChatClient(chatClient);
        String tokenValue = StpUtil.getTokenValue();

        // 设置流式消息消费者，用于实时发送SSE消息
        setupStreamMessageConsumer(emitter, context);

        CompletableFuture.runAsync(() -> {
            SaTokenContextMockUtil.setMockContext(() -> {
                StpUtil.setTokenValueToStorage(tokenValue);
                try {
                    if (context.getState() != AgentState.IDLE) {
                        sendError(emitter, "错误, 无法从此状态运行代理: " + context.getState());
                        return;
                    }
                    if (StringUtil.isBlank(userPrompt)) {
                        sendError(emitter, "错误, 不能使用空提示词运行代理");
                        return;
                    }

                    context.setState(AgentState.RUNNING);
                    context.getMessageList().add(new UserMessage(userPrompt));

                    try {
                        for (int i = 0; i < maxSteps && context.getState() != AgentState.FINISHED; i++) {
                            if (context.getState() == AgentState.ERROR){
                                sendError(emitter,"网络故障，请稍后重新");
                            }
                            int stepNumber = i + 1;
                            context.setCurrentStep(stepNumber);
                            log.info("Executing step " + stepNumber + "/" + maxSteps);

                            String stepResult = step(context);

                            // 注意：流式消息现在通过 setupStreamMessageConsumer 设置的消费者实时发送
                            // 这里不再需要从 agentMessageQueue 处理消息，避免重复发送

                            //发送步骤结果作为工具消息
                            if (stepResult != null && !stepResult.isEmpty()) {
                                sendMessage(emitter, "toolResponse", "Step " + stepNumber + ": " + stepResult);
                            }
                        }

                        if (context.getCurrentStep() >= maxSteps) {
                            context.setState(AgentState.FINISHED);
                            sendMessage(emitter, "message", "执行结束: 达到最大步骤 (" + maxSteps + ")");
                        }

                        // 发送完成事件
                        completeEmitter(emitter);
                    } catch (Exception e) {
                        context.setState(AgentState.ERROR);
                        log.error("执行智能体失败", e);
                        sendError(emitter, "执行错误: " + e.getMessage());
                    } finally {
                        cleanup(context);
                    }
                } catch (Exception e) {
                    sendError(emitter, "系统错误: " + e.getMessage());
                } finally {
                    cleanup(context);
                }
            });
        });

        emitter.onTimeout(() -> {
            context.setState(AgentState.ERROR);
            cleanup(context);
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (context.getState() == AgentState.RUNNING) {
                context.setState(AgentState.FINISHED);
            }
            cleanup(context);
            log.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * 设置流式消息消费者，子类可以重写此方法以支持实时流式消息发送
     */
    protected void setupStreamMessageConsumer(SseEmitter emitter, AgentContext context) {
        // 默认实现为空，ToolCallAgent 会重写此方法
    }

    /**
     * 发送带类型的SSE消息
     */
    private void sendMessage(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("发送SSE消息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(JsonUtils.toJson(message)));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }

    /**
     * 发送完成事件
     */
    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("Agent响应完成"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public abstract String step(AgentContext context);

    protected void cleanup(AgentContext context) {

    }
}
