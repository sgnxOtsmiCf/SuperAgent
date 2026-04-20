package cn.sgnxotsmicf.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2025/9/4 14:45
 * @Version: 1.0
 * @Description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedissonConfig {

    private String host;

    private Integer port;

    private String username;

    private String password;

    private Integer database;


    @Bean
    public Config redissonConfigInstance() {
        Config config = new Config();
        config.setCodec(new StringCodec());
        config.useSingleServer()
                .setAddress("redis://"+host+":"+port)
                .setPassword(password)
                .setDatabase(database)
                .setUsername(username);
        return config;
    }

    @Bean
    public RedissonClient redissonClient(Config redissonConfigInstance) {
        return Redisson.create(redissonConfigInstance);
    }
}
