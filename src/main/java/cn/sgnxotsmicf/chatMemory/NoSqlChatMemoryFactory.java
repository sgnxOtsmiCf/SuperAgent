package cn.sgnxotsmicf.chatMemory;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.redisson.config.Config;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 10:43
 * @Version: 1.0
 * @Description:
 */

@Component
public class NoSqlChatMemoryFactory {



    public ChatMemoryRepository getRedissonRedisChatMemoryRepository(Config redissonConfigInstance, Long userId, Long agentId) {
        return CustomRedissonRedisChatMemoryRepository.builder()
                .redissonConfig(redissonConfigInstance)
                .keyPrefix("openManus:"+ userId + ":" + agentId + ":")
                //.redissonConfig(new Config())
                .build();
    }


    public ChatMemory getRedissonChatMemory(ChatMemoryRepository redissonRedisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redissonRedisChatMemoryRepository)
                .maxMessages(20)
                .build();
    }


}
