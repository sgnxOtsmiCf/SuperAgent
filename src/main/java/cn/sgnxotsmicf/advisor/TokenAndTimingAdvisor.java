package cn.sgnxotsmicf.advisor;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;


/**
 * @Author: lixiang
 * @CreateDate: 2026/5/5 01:03
 * @Version: 1.0
 * @Description:
 */
@Component
public class TokenAndTimingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenAndTimingAdvisor.class);

    @NotNull
    @Override
    public String getName() {
        return "tokenAndTimingAdvisor";
    }

    /**
     * 如果你希望“包住整个链路”（包括 RAG、memory、tool 等所有 advisor 的耗时），
     * 就让它尽量靠前执行（order 更小 = 更靠前）
     * Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER 的语义是给用户留出优先级空间。7
     */
    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 500;
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest request, @NotNull CallAdvisorChain chain) {
        long startNs = System.nanoTime();

        ChatClientResponse response = null;
        Throwable error = null;

        try {
            // 继续调用链路：nextCall() 8
            response = chain.nextCall(request);
            return response;
        }
        catch (Throwable t) {
            error = t;
            throw t;
        }
        finally {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            Usage usage = extractUsage(response);
            Integer prompt = usage != null ? usage.getPromptTokens() : null;
            Integer completion = usage != null ? usage.getCompletionTokens() : null;
            Integer total = usage != null ? usage.getTotalTokens() : null;

            if (error == null) {
                log.info("[AI call] costMs={} promptTokens={} completionTokens={} totalTokens={}",
                        costMs, prompt, completion, total);
            } else {
                log.warn("[AI call] costMs={} promptTokens={} completionTokens={} totalTokens={} error={}",
                        costMs, prompt, completion, total, error.toString());
            }

            // 这里你也可以接 Micrometer：timer.record(costMs, MILLISECONDS) / counter.increment(total) 等
        }
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest request, StreamAdvisorChain chain) {
        long startNs = System.nanoTime();

        // 继续调用链路：nextStream()
        Flux<ChatClientResponse> upstream = chain.nextStream(request);

        // 推荐：聚合流式结果，拿到“最终 ChatClientResponse”，再统计 tokens 更稳定 10
        ChatClientMessageAggregator aggregator = new ChatClientMessageAggregator();

        return aggregator.aggregateChatClientResponse(upstream, aggregatedResponse -> {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            Usage usage = extractUsage(aggregatedResponse);
            Integer prompt = usage != null ? usage.getPromptTokens() : null;
            Integer completion = usage != null ? usage.getCompletionTokens() : null;
            Integer total = usage != null ? usage.getTotalTokens() : null;

            log.info("[AI stream] costMs={} promptTokens={} completionTokens={} totalTokens={}",
                    costMs, prompt, completion, total);

            // 同样：这里可以上报指标
        });
    }

    private Usage extractUsage(ChatClientResponse response) {
        if (response == null) return null;

        // ChatClientResponse.chatResponse() 可能为 null
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) return null;

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        // ChatResponseMetadata.getUsage()
        return metadata.getUsage();
    }
}