package cn.sgnxotsmicf.advisor;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/19 14:19
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Component
public class AgentLogAdvisor implements CallAdvisor, StreamAdvisor {


    @NotNull
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        String contents = chatClientRequest.prompt().getUserMessage().getText();
        log.info("用户输入: {}", contents);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        assert chatClientResponse.chatResponse() != null;
        log.info("模型回答: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        return chatClientResponse;
    }


    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        String contents = chatClientRequest.prompt().getUserMessage().getText();
        log.info("用户输入: {}", contents);
        Flux<ChatClientResponse> chatClientResponseFlux = streamAdvisorChain.nextStream(chatClientRequest);
        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponseFlux, chatClientResponse -> {
            assert chatClientResponse.chatResponse() != null;
            log.info("模型回答: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        });
    }


    @NotNull
    @Override
    public String getName() {
        return this.getClass().getName();
    }


    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 1;
    }
}
