package cn.sgnxotsmicf.app.manus;

import cn.hutool.core.collection.CollUtil;
import cn.sgnxotsmicf.app.manus.model.AgentContext;
import cn.sgnxotsmicf.app.manus.model.AgentState;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final ToolCallback[] availableTools;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .enableSearch(true)
                .enableThinking(true)
                .thinkingBudget(500)
                .internalToolExecutionEnabled(false)
                .build();
    }



    /**
     * 发送流式消息，如果消费者存在则直接发送，否则添加到上下文队列
     */
    private void sendStreamMessage(AgentContext context, AgentMessage message) {
        Consumer<AgentMessage> consumer = context.getStreamMessageConsumer();
        if (consumer != null) {
            try {
                consumer.accept(message);
            } catch (Exception e) {
                log.warn("实时发送流式消息失败，回退到队列模式: {}", e.getMessage());
                addMessageToContext(context, message);
            }
        } else {
            addMessageToContext(context, message);
        }
    }

    /**
     * 根据消息类型添加到上下文
     */
    private void addMessageToContext(AgentContext context, AgentMessage message) {
        switch (message.getType()) {
            case "thinking" -> context.addThinkingMessage(message.getData());
            case "message" -> context.addMessage(message.getData());
            case "toolUsage" -> context.addToolUsageMessage(message.getData());
            case "toolResponse" -> context.addToolResponseMessage(message.getData());
            case "error" -> context.addErrorMessage(message.getData());
            default -> context.addMessage(message.getData());
        }
    }



    @Override
    public boolean think(AgentContext context) {
//        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty() && context.getCurrentStep() != 1) {
//            UserMessage userMessage = new UserMessage(getNextStepPrompt());
//            context.getMessageList().add(userMessage);
//        } //TODO: 不使用这个，改使用拦截器Advisor进行统一处理
        List<Message> messageList = context.getMessageList();
        Prompt prompt = new Prompt(messageList);
        try {
            // 用于收集完整响应
            AtomicReference<StringBuilder> reasoningBuilder = new AtomicReference<>(new StringBuilder());
            AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
            AtomicReference<List<AssistantMessage.ToolCall>> toolCallsRef = new AtomicReference<>(new ArrayList<>());
            AtomicReference<Map<String, Object>> metadataRef = new AtomicReference<>(new HashMap<>());

            // 使用流式调用
            Flux<ChatResponse> streamResponse = context.getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, context.getSessionId())
                            .param("retrieveSize", 10)
                            .param("maxHistorySize", 15))
                    .stream()
                    .chatResponse()
                    .doOnNext(chunk -> {
                        if (chunk != null && chunk.getResult() != null) {
                            Generation result = chunk.getResult();
                            AssistantMessage output = result.getOutput();

                            // 处理思考内容（reasoningContent）
                            Object reasoningContent = output.getMetadata().get("reasoningContent");
                            if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                                String reasoning = reasoningContent.toString();
                                reasoningBuilder.get().append(reasoning);
                                // 实时发送思考内容
                                sendStreamMessage(context, AgentMessage.thinking(reasoning));
                                log.info("{}的思考: {}", getName(), reasoning);
                            }

                            // 处理普通消息内容
                            String text = output.getText();
                            if (text != null && !text.isEmpty()) {
                                messageBuilder.get().append(text);
                                // 实时发送消息内容
                                sendStreamMessage(context, AgentMessage.message(text));
                                log.info("{}的响应: {}", getName(), text);
                            }

                            // 收集工具调用信息（只在有工具调用时收集一次）
                            if (output.hasToolCalls() && toolCallsRef.get().isEmpty()) {
                                toolCallsRef.set(output.getToolCalls());
                            }
                            metadataRef.set(output.getMetadata());
                        }
                    })
                    .doOnError(error -> {
                        log.error("流式调用出错: ", error);
                        sendStreamMessage(context, AgentMessage.error("流式调用出错: " + error.getMessage()));
                        context.setState(AgentState.ERROR);
                    })
                    .doOnComplete(() ->
                            log.info("{}流式响应完成", getName())
                    );

            // 等待流式响应完成（阻塞等待）- 只使用 blockLast
            ChatResponse finalResponse = streamResponse.blockLast();

            // 构建 AssistantMessage的信息
            String fullMessage = messageBuilder.get().toString();
            List<AssistantMessage.ToolCall> allToolCalls = toolCallsRef.get();
            Map<String, Object> metadata = metadataRef.get();
            metadata.put("reasoningContent",reasoningBuilder.get().toString());
            // 使用 Builder 构建完整的 AssistantMessage
            AssistantMessage completeMessage = AssistantMessage.builder()
                    .content(fullMessage)
                    .toolCalls(allToolCalls)
                    .properties(metadata != null ? metadata : new HashMap<>())
                    .build();

            // 如果有工具调用，发送工具调用信息
            if (!allToolCalls.isEmpty()) {
                //context.addMessage(getName() + "选择了 " + allToolCalls.size() + " 个工具来使用");

                // 发送详细的工具调用信息作为toolUsage类型
                List<ToolCallInfo> toolCallInfos = allToolCalls.stream()
                        .map(toolCall -> new ToolCallInfo(toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.toList());
                sendStreamMessage(context, AgentMessage.toolUsage(toolCallInfos));

                // 记录日志
                String toolCallInfoStr = allToolCalls.stream()
                        .map(toolCall -> String.format("工具名称：%s，参数：%s",
                                toolCall.name(),
                                toolCall.arguments())
                        )
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfoStr);
            }

            // 创建完整的 ChatResponse 保存到 context
            if (finalResponse != null) {
                context.setToolCallChatResponse(finalResponse);
            }

            // 将完整消息添加到消息列表
            context.getMessageList().add(completeMessage);

            // 返回是否需要执行工具调用
            return !allToolCalls.isEmpty();

        } catch (Exception e) {
            log.error("think 方法执行出错: ", e);
            // 使用 Builder 创建错误消息
            AssistantMessage errorMessage = AssistantMessage.builder()
                    .content("处理时遇到错误: " + e.getMessage())
                    .build();
            context.getMessageList().add(errorMessage);
            String messageError = getName() + "的思考过程遇到了问题: " + e.getMessage();
            log.error(messageError);
            sendStreamMessage(context, AgentMessage.error(messageError));
            return false;
        }
    }


    @Override
    public String act(AgentContext context) {
        if (!context.getToolCallChatResponse().hasToolCalls()) {
            return "没有工具调用";
        }
        Prompt prompt = new Prompt(context.getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, context.getToolCallChatResponse());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        //TODO:确认是否可以手动维护
        context.getMessageList().add(toolResponseMessage);
//        ArrayList<Message> messages = new ArrayList<>();
//        messages.add(toolResponseMessage);
//        context.setMessageList(messages);

        // 构建工具响应结果列表
        List<ToolResponseInfo> toolResponses = toolResponseMessage.getResponses().stream()
                .map(response -> new ToolResponseInfo(response.name(), response.responseData()))
                .collect(Collectors.toList());

        // 发送工具响应消息（通过流式消费者实时发送）
        sendStreamMessage(context, AgentMessage.toolResponse(toolResponses));

        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        boolean TerminateToolBool =
                toolResponseMessage.getResponses().stream()
                        .anyMatch(response -> "doTerminate".equals(response.name()));
        if (TerminateToolBool) {
            context.setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;
    }


    /**
     * 设置流式消息消费者，将消息实时发送到SSE
     */
    @Override
    protected void setupStreamMessageConsumer(SseEmitter emitter, AgentContext context) {
        context.setStreamMessageConsumer(message -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(message.getType())
                        .data(message.getData()));
            } catch (IOException e) {
                log.error("发送SSE消息失败: {}", e.getMessage());
                throw new RuntimeException("发送SSE消息失败", e);
            }
        });
    }


    /**
     * 工具调用信息内部类
     */
    @Data
    public static class ToolCallInfo {
        private String name;
        private String arguments;

        public ToolCallInfo(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }

    /**
     * 工具响应信息内部类
     */
    @Data
    public static class ToolResponseInfo {
        private String name;
        private String responseData;

        public ToolResponseInfo(String name, String responseData) {
            this.name = name;
            this.responseData = responseData;
        }
    }
}
