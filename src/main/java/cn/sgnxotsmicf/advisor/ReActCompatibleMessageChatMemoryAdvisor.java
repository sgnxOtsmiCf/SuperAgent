package cn.sgnxotsmicf.advisor;

import org.jetbrains.annotations.NotNull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class ReActCompatibleMessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final ChatMemory chatMemory;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> reasoningContentAccumulators = new ConcurrentHashMap<>();

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

        List<Message> processedMessages;
        if (isReActIteration(instructions)) {
            processedMessages = new ArrayList<>(instructions);
        } else {
            List<Message> memoryMessages = this.chatMemory.get(conversationId);
            processedMessages = new ArrayList<>(memoryMessages);
            processedMessages.addAll(instructions);
        }

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

        if (!isReActIteration(instructions) || processedMessages.getLast() instanceof ToolResponseMessage) {
            Message userMessage = processedChatClientRequest.prompt().getLastUserOrToolResponseMessage();
            this.chatMemory.add(conversationId, userMessage);
        }

        return processedChatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return persistWithReasoningContent(chatClientResponse, null);
    }

    private ChatClientResponse persistWithReasoningContent(ChatClientResponse chatClientResponse, String streamConversationId) {
        String conversationId = streamConversationId != null
                ? streamConversationId
                : this.getConversationId(chatClientResponse.context(), this.defaultConversationId);

        List<AssistantMessage> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse().getResults().stream()
                    .map(g -> g.getOutput())
                    .toList();
        }

        ConcurrentLinkedQueue<String> accumulator = streamConversationId != null
                ? reasoningContentAccumulators.remove(conversationId)
                : null;
        String accumulatedReasoning = (accumulator != null && !accumulator.isEmpty())
                ? String.join("", accumulator)
                : null;

        List<Message> messageList = assistantMessages.stream().map(assistantMessage -> {
            if (accumulatedReasoning != null && !accumulatedReasoning.isEmpty()) {
                Map<String, Object> newMetadata = new HashMap<>(assistantMessage.getMetadata());
                newMetadata.put("reasoningContent", accumulatedReasoning);
                return AssistantMessage.builder()
                        .content(assistantMessage.getText())
                        .toolCalls(assistantMessage.getToolCalls())
                        .properties(newMetadata)
                        .build();
            }
            Object existingReasoning = assistantMessage.getMetadata().get("reasoningContent");
            if (existingReasoning == null || existingReasoning.toString().isEmpty()) {
                Map<String, Object> newMetadata = new HashMap<>(assistantMessage.getMetadata());
                enrichReasoningFromSubclass(newMetadata, assistantMessage);
                if (newMetadata.containsKey("reasoningContent")) {
                    return AssistantMessage.builder()
                            .content(assistantMessage.getText())
                            .toolCalls(assistantMessage.getToolCalls())
                            .properties(newMetadata)
                            .build();
                }
            }
            return assistantMessage;
        }).collect(Collectors.toList());

        this.chatMemory.add(conversationId, messageList);
        return chatClientResponse;
    }

    private void enrichReasoningFromSubclass(Map<String, Object> metadata, AssistantMessage assistantMessage) {
        try {
            if (assistantMessage.getClass().getMethod("getReasoningContent") != null) {
                Object reasoningContent = assistantMessage.getClass()
                        .getMethod("getReasoningContent")
                        .invoke(assistantMessage);
                if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                    metadata.put("reasoningContent", reasoningContent.toString());
                }
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("提取reasoningContent失败: {}", e.getMessage());
        }
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, @NotNull StreamAdvisorChain streamAdvisorChain) {
        Scheduler scheduler = this.getScheduler();
        String conversationId = this.getConversationId(chatClientRequest.context(), this.defaultConversationId);

        reasoningContentAccumulators.put(conversationId, new ConcurrentLinkedQueue<>());

        Mono<ChatClientRequest> var10000 = Mono.just(chatClientRequest).publishOn(scheduler).map((request) -> this.before(request, streamAdvisorChain));
        Objects.requireNonNull(streamAdvisorChain);
        return var10000.flatMapMany(streamAdvisorChain::nextStream)
                .doOnNext(chatClientResponse -> {
                    if (chatClientResponse.chatResponse() != null) {
                        chatClientResponse.chatResponse().getResults().forEach(generation -> {
                            AssistantMessage output = generation.getOutput();
                            Object reasoningContent = output.getMetadata().get("reasoningContent");
                            if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                                ConcurrentLinkedQueue<String> queue = reasoningContentAccumulators.get(conversationId);
                                if (queue != null) {
                                    queue.add(reasoningContent.toString());
                                }
                            }
                        });
                    }
                })
                .doOnError(error -> reasoningContentAccumulators.remove(conversationId))
                .transform((flux) -> (new ChatClientMessageAggregator()).aggregateChatClientResponse(flux,
                        (response) -> this.persistWithReasoningContent(response, conversationId)));
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
