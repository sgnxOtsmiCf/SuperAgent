package cn.sgnxotsmicf.advisor;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 兼容 ReAct 模式的 MessageChatMemoryAdvisor。
 *
 * 在 ReAct（think/act 循环）场景下，用户一次输入会触发多轮 LLM 调用，
 * 原版 MessageChatMemoryAdvisor 的 before() 每次都会将用户消息存入 memory，
 * 导致用户消息或工具消息被重复存储。
 *
 * 本类的改进：通过判断当前 prompt 的 instructions 中是否包含 AssistantMessage
 * 或 ToolResponseMessage 来识别是否处于 ReAct 循环中：
 * - 首次请求（instructions 中只有 UserMessage/SystemMessage）：正常保存用户消息
 * - ReAct 循环中的后续请求（instructions 中已有 AssistantMessage/ToolResponseMessage）：
 *   只加载历史到上下文，不重复保存用户消息
 */
public final class ReActCompatibleMessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final ChatMemory chatMemory;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;

    private ReActCompatibleMessageChatMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId, int order, Scheduler scheduler) {
        Assert.notNull(chatMemory, "chatMemory cannot be null");
        Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.chatMemory = chatMemory;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * 判断当前请求是否处于 ReAct 循环中。
     * 如果 instructions 中包含 AssistantMessage 或 ToolResponseMessage
     * 说明这是 ReAct 循环中的后续迭代，而非用户的首次请求。
     */
    private boolean isReActIteration(List<Message> instructions) {
        for (Message message : instructions) {
            if (message instanceof AssistantMessage || message instanceof ToolResponseMessage) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String conversationId = this.getConversationId(chatClientRequest.context(), this.defaultConversationId);
        List<Message> instructions = chatClientRequest.prompt().getInstructions();

        // 合并历史消息和当前指令
        List<Message> processedMessages;
        if (isReActIteration(instructions)) {
            // ReAct 循环中的后续请求：直接使用 instructions，不合并 memory 历史
            // 因为 messageList 已经维护了完整的对话上下文
            processedMessages = new ArrayList<>(instructions);

        }else {
            // 首次请求：合并 memory 历史 + 当前 instructions
            List<Message> memoryMessages = this.chatMemory.get(conversationId);
            processedMessages = new ArrayList<>(memoryMessages);
            processedMessages.addAll(instructions);
        }

        // 将 SystemMessage 移到最前面
        for (int i = 0; i < processedMessages.size(); ++i) {
            if (processedMessages.get(i) instanceof SystemMessage) {
                Message systemMessage = processedMessages.remove(i);
                processedMessages.add(0, systemMessage);
                break;
            }
        }

        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
                .build();

        // 核心：只在首次用户请求时保存用户消息，确保 ReAct 循环中不重复保存
        if (!isReActIteration(instructions) || processedMessages.getLast() instanceof ToolResponseMessage) {
            Message userMessage = processedChatClientRequest.prompt().getLastUserOrToolResponseMessage();
            this.chatMemory.add(conversationId, userMessage);
        }

        return processedChatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        List<AssistantMessage> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse().getResults().stream()
                    .map(g -> g.getOutput())
                    .toList();
        }
        List<Message> messageList = assistantMessages.stream().map(assistantMessage -> {
            Message message = assistantMessage;
            return message;
        }).collect(Collectors.toList());
        this.chatMemory.add(this.getConversationId(chatClientResponse.context(), this.defaultConversationId), messageList);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Scheduler scheduler = this.getScheduler();
        Mono<ChatClientRequest> var10000 = Mono.just(chatClientRequest).publishOn(scheduler).map((request) -> this.before(request, streamAdvisorChain));
        Objects.requireNonNull(streamAdvisorChain);
        return var10000.flatMapMany(streamAdvisorChain::nextStream)
                .transform((flux) -> (new ChatClientMessageAggregator()).aggregateChatClientResponse(flux, (response) -> this.after(response, streamAdvisorChain)));
    }

    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    public static final class Builder {
        private String conversationId = "default";
        private int order = BaseAdvisor.HIGHEST_PRECEDENCE;
        private Scheduler scheduler;
        private ChatMemory chatMemory;

        private Builder(ChatMemory chatMemory) {
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
            this.chatMemory = chatMemory;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public ReActCompatibleMessageChatMemoryAdvisor build() {
            return new ReActCompatibleMessageChatMemoryAdvisor(this.chatMemory, this.conversationId, this.order, this.scheduler);
        }
    }
}
