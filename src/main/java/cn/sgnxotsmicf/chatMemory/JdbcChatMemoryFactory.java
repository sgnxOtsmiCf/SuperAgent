package cn.sgnxotsmicf.chatMemory;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/2 14:47
 * @Version: 1.0
 * @Description:
 */
@Component
public class JdbcChatMemoryFactory {

    @Resource(name = "mySqlJdbcTemplate")
    private JdbcTemplate mySqljdbcTemplate;

    @Resource(name = "pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Resource
    private MySqlChatMemoryRepository mySqlChatMemoryRepository;

    @Bean
    public JdbcChatMemoryRepository originMySqLMemoryRepository() {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(mySqljdbcTemplate)
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();
    }

    @Bean
    public JdbcChatMemoryRepository pgVectorMemoryRepository() {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(pgVectorJdbcTemplate)
                .dialect(new PostgresChatMemoryRepositoryDialect())
                .build();
    }

    @Bean
    public ChatMemory originMySqlChatMemory(JdbcChatMemoryRepository originMySqLMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(originMySqLMemoryRepository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatMemory pgVectorChatMemory(JdbcChatMemoryRepository pgVectorMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(pgVectorMemoryRepository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatMemory mySqlChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(mySqlChatMemoryRepository)
                .maxMessages(10)
                .build();
    }

}
