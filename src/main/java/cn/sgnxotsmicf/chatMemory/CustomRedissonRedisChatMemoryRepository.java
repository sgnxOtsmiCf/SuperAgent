package cn.sgnxotsmicf.chatMemory;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import cn.sgnxotsmicf.common.tools.AgentCommon;
import cn.sgnxotsmicf.common.tools.SessionIdUtil;
import com.alibaba.cloud.ai.memory.redis.BaseRedisChatMemoryRepository;
import com.alibaba.cloud.ai.memory.redis.builder.RedisChatMemoryBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.redisson.Redisson;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RListAsync;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

// 修改类名：从 RedissonRedisChatMemoryRepository 改为 CustomRedissonRedisChatMemoryRepository
public class CustomRedissonRedisChatMemoryRepository extends BaseRedisChatMemoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomRedissonRedisChatMemoryRepository.class);
    private final RedissonClient redissonClient;

    private CustomRedissonRedisChatMemoryRepository(RedissonClient redissonClient) {
        Assert.notNull(redissonClient, "redissonClient cannot be null");
        this.redissonClient = redissonClient;
    }

    // 修改返回类型：从 RedissonBuilder 改为 CustomRedissonBuilder
    public static CustomRedissonBuilder builder() {
        return new CustomRedissonBuilder();
    }

    public List<String> findConversationIds() {
        RKeys keys = this.redissonClient.getKeys();
        KeysScanOptions scanOptions = KeysScanOptions.defaults().pattern(this.getKeyPrefix() + "*");
        Iterable<String> keysIter = keys.getKeys(scanOptions);
        return (List)StreamSupport.stream(keysIter.spliterator(), false).map((key) -> key.substring(this.getKeyPrefix().length())).collect(Collectors.toList());
    }

    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        RedissonClient var10000 = this.redissonClient;
        String var10001 = this.getKeyPrefix();
        RList<String> redisList = var10000.getList(var10001 + conversationId);
        return (List)redisList.readAll().parallelStream().map(this::deserializeMessage).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<String> findPageConversationIds(Long userId, String redisKeyPrefix, int pageNo, int pageSize, StringRedisTemplate stringRedisTemplate) {
        int offset = (pageNo - 1) * pageSize;
        Set<String> sessionList = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScore(redisKeyPrefix + userId, 0, System.currentTimeMillis(), offset, pageSize);
        assert sessionList != null;
        return sessionList.stream().toList();
    }

    // 新增方法：分页查询对话，使用管道批量获取指定页码的对话数据，减少Redis交互次数
    public Map<String, List<Message>> getConversations(List<String> pageConversationIds) {

        if (pageConversationIds == null || pageConversationIds.isEmpty()) {
            pageConversationIds = findConversationIds();
        }

        // 创建管道，批量读取当前页的列表数据，只发起一次网络请求
        RBatch batch = this.redissonClient.createBatch();
        // 显式指定RListAsync类型为String，避免类型推断为Object
        Map<String, RFuture<List<String>>> futures = pageConversationIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            RListAsync<String> listAsync = batch.getList(this.getKeyPrefix() + id);
                            return listAsync.readAllAsync();
                        }
                ));

        // 执行批量读取
        batch.execute();

        // 解析结果：通过RFuture.get()获取异步结果
        return pageConversationIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        List<String> serialized = futures.get(id).get();
                        return serialized.stream()
                                .map(this::deserializeMessage)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get conversation: " + id, e);
                    }
                }
        ));
    }



    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");
        RedissonClient var10000 = this.redissonClient;
        String var10001 = this.getKeyPrefix();
        RList<String> redisList = var10000.getList(var10001 + conversationId);
        redisList.delete();
        List<String> serializedMessages = messages.stream().map(this::serializeMessage).toList();
        redisList.addAll(serializedMessages);
    }

    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        RedissonClient var10000 = this.redissonClient;
        String var10001 = this.getKeyPrefix();
        RList<String> redisList = var10000.getList(var10001 + conversationId);
        redisList.delete();
    }

    // 新增方法：使用管道批量删除多个对话，只与Redis交互一次，减少网络传输损耗
    public void deleteByConversationIds(List<String> conversationIds) {
        Assert.notNull(conversationIds, "conversationIds cannot be null");
        if (conversationIds.isEmpty()) {
            return;
        }

        // 创建管道，批量删除所有列表，只发起一次网络请求
        RBatch batch = this.redissonClient.createBatch();
        conversationIds.forEach(id -> {
            // 显式指定RListAsync类型为String
            RListAsync<String> listAsync = batch.getList(this.getKeyPrefix() + id);
            listAsync.deleteAsync();
        });
        batch.execute();
    }

    public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        String var10000 = this.getKeyPrefix();
        String key = var10000 + conversationId;
        RList<Object> list = this.redissonClient.getList(key);
        int size = list.size();
        if (size >= maxLimit) {
            list.trim(deleteSize, -1);
        }
    }

    public void close() {
        if (this.redissonClient != null && !this.redissonClient.isShutdown()) {
            try {
                int activeConnections = this.redissonClient.getConfig().getNettyThreads();
                logger.info("Shutting down Redisson with {} active connections", activeConnections);
                this.redissonClient.shutdown();
                logger.info("Redisson client shutdown completed");
            } catch (Exception e) {
                logger.error("Error shutting down Redisson client", e);
            }
        }

    }

    // 修改内部类名：从 RedissonBuilder 改为 CustomRedissonBuilder
    public static class CustomRedissonBuilder extends RedisChatMemoryBuilder<CustomRedissonBuilder> {
        private int poolSize = 32;
        private Config redissonConfig;

        public CustomRedissonBuilder() {
        }

        protected CustomRedissonBuilder self() {
            return this;
        }

        public CustomRedissonBuilder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public CustomRedissonBuilder redissonConfig(Config redissonConfig) {
            this.redissonConfig = redissonConfig;
            return this;
        }

        // 修改返回类型：从 RedissonRedisChatMemoryRepository 改为 CustomRedissonRedisChatMemoryRepository
        public CustomRedissonRedisChatMemoryRepository build() {
            BaseRedisChatMemoryRepository.CUSTOM_KEY_PREFIX = this.keyPrefix;
            if (this.redissonConfig != null) {
                if (this.redissonConfig.getCodec() == null) {
                    this.redissonConfig.setCodec(new StringCodec());
                }

                return new CustomRedissonRedisChatMemoryRepository(Redisson.create(this.redissonConfig));
            } else {
                Config config = new Config();
                config.setCodec(new StringCodec());
                if (this.useCluster) {
                    List<String> nodesUrl = this.nodes.stream().map((node) -> "redis://" + node).toList();
                    if (this.useSsl && StringUtils.hasText(this.bundle)) {
                        if (this.sslBundles == null) {
                            throw new IllegalStateException("spring.ssl configuration is required when use SSL in redis chat memory");
                        }

                        SslBundle sslBundle = this.sslBundles.getBundle(this.bundle);
                        SslManagerBundle managers = sslBundle.getManagers();
                        config.useClusterServers().setSslTrustManagerFactory(managers.getTrustManagerFactory());
                        nodesUrl = this.nodes.stream().map((node) -> "rediss://" + node).toList();
                    }

                    ((ClusterServersConfig)((ClusterServersConfig)config.useClusterServers().addNodeAddress((String[])nodesUrl.toArray(new String[0])).setConnectTimeout(this.timeout)).setSlaveConnectionPoolSize(this.poolSize)).setMasterConnectionPoolSize(this.poolSize);
                    if (StringUtils.hasLength(this.username)) {
                        config.useClusterServers().setUsername(this.username);
                    }

                    if (StringUtils.hasLength(this.password)) {
                        config.useClusterServers().setPassword(this.password);
                    }
                } else {
                    String nodeUrl = "redis://" + this.host + ":" + this.port;
                    if (this.useSsl && StringUtils.hasText(this.bundle)) {
                        if (this.sslBundles == null) {
                            throw new IllegalStateException("spring.ssl configuration is required when use SSL in redis chat memory");
                        }

                        SslBundle sslBundle = this.sslBundles.getBundle(this.bundle);
                        SslManagerBundle managers = sslBundle.getManagers();
                        config.useSingleServer().setSslTrustManagerFactory(managers.getTrustManagerFactory());
                        nodeUrl = "rediss://" + this.host + ":" + this.port;
                    }

                    ((SingleServerConfig)config.useSingleServer().setAddress(nodeUrl).setConnectionPoolSize(this.poolSize).setConnectTimeout(this.timeout)).setDatabase(this.database);
                    if (StringUtils.hasLength(this.username)) {
                        config.useSingleServer().setUsername(this.username);
                    }

                    if (StringUtils.hasLength(this.password)) {
                        config.useSingleServer().setPassword(this.password);
                    }
                }

                return new CustomRedissonRedisChatMemoryRepository(Redisson.create(config));
            }
        }
    }
}