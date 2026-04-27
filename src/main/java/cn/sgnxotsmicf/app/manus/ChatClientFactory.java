package cn.sgnxotsmicf.app.manus;

import cn.sgnxotsmicf.advisor.ReActCompatibleMessageChatMemoryAdvisor;
import cn.sgnxotsmicf.advisor.VectorStoreFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/9 21:27
 * @Version: 1.0
 * @Description:
 */

@Component
public class ChatClientFactory {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private VectorStore pgVectorStore;


    public ChatClient getChatClient(ChatMemory redissonChatMemory) {
        return ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(redissonChatMemory).build())
                .build();
    }

    public ChatClient getChatClient(ChatModel chatModel, ChatMemory redissonChatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(redissonChatMemory).build())
                .build();
    }

    /**
     * reAct模式
     * @param userId 用户id
     * @param userName 用户名
     * @param redissonChatMemory redis的记忆存储
     * @param advisors 拦截器组
     * @return ChatClient对象
     */
    public ChatClient getChatClient(Long userId, String userName, ChatMemory redissonChatMemory, List<Advisor> advisors) {
        return ChatClient.builder(dashscopeChatModel)
                .defaultToolContext(Map.of("userId", userId,"userName", userName))
                .defaultAdvisors(ReActCompatibleMessageChatMemoryAdvisor.builder(redissonChatMemory).build())
                .defaultAdvisors(advisors)
                .build();
    }

    public ChatClient getChatClient(Long userId, String userName, ChatModel chatModel, ChatMemory redissonChatMemory) {
        return ChatClient.builder(chatModel)
                .defaultToolContext(Map.of("userId", userId,"userName", userName))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(redissonChatMemory).build())
                .build();
    }

    public ChatClient getChatClient(Long userId, String userName, ChatModel chatModel,ChatMemory redissonChatMemory, boolean isRag) {
        if (!isRag) {
            return getChatClient(userId, userName, chatModel, redissonChatMemory);
        }
        return ChatClient.builder(chatModel)
                .defaultToolContext(Map.of("userId", userId,"userName", userName))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(redissonChatMemory).build())
                .defaultAdvisors(
                        VectorStoreFactory
                                .createPGVectorAdvisor(VectorStoreFactory
                                        .createVectorStoreDocumentRetriever(pgVectorStore,0.75,4))
                )
                .build();
    }

    /**
     * ReAct模式
     * @param userId 用户id
     * @param userName 用户名
     * @param redissonChatMemory redis的记忆存储
     * @param advisors 拦截器组
     * @param isRag 是否开启rag
     * @return ChatClient对象
     */
    public ChatClient getChatClient(Long userId, String userName, ChatMemory redissonChatMemory, List<Advisor> advisors, boolean isRag) {
        if (!isRag) {
            return getChatClient(userId, userName, redissonChatMemory, advisors);
        }
        return ChatClient.builder(dashscopeChatModel)
                .defaultToolContext(Map.of("userId", userId, "userName", userName))
                .defaultAdvisors(ReActCompatibleMessageChatMemoryAdvisor
                        .builder(redissonChatMemory)
                        .build())
                .defaultAdvisors(
                        VectorStoreFactory
                                .createPGVectorAdvisor(VectorStoreFactory
                                        .createVectorStoreDocumentRetriever(pgVectorStore, 0.75, 4))
                )
                .defaultAdvisors(advisors)
                .build();
    }

    public ChatClient getReActNextClient(Long userId, String userName, ChatMemory redissonChatMemory, List<Advisor> advisors, boolean isRag) {
        if (!isRag) {
            return getChatClient(userId, userName, dashscopeChatModel, redissonChatMemory);
        }
        return ChatClient.builder(dashscopeChatModel)
                .defaultToolContext(Map.of("userId", userId,"userName", userName))
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(redissonChatMemory)
                        .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                        .build())
                .defaultAdvisors(
                        VectorStoreFactory
                                .createPGVectorAdvisor(VectorStoreFactory
                                        .createVectorStoreDocumentRetriever(pgVectorStore,0.75,4))
                )
                .defaultAdvisors(advisors)
                .build();
    }

}
