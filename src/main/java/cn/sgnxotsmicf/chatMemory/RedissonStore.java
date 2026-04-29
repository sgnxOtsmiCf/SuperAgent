package cn.sgnxotsmicf.chatMemory;

import com.alibaba.cloud.ai.graph.store.*;
import com.alibaba.cloud.ai.graph.store.constant.StoreConstant;
import com.alibaba.cloud.ai.graph.store.stores.BaseStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 21:18
 * @Version: 1.0
 * @Description: 自实现Redis Store
 */

public class RedissonStore extends BaseStore {

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final String keyPrefix;

    /**
     * Constructor with default key prefix.
     *
     * @param redissonClient Redisson client instance
     */
    public RedissonStore(RedissonClient redissonClient) {
        this(redissonClient, StoreConstant.REDIS_KEY_PREFIX);
    }

    /**
     * Constructor with custom key prefix.
     *
     * @param redissonClient Redisson client instance
     * @param keyPrefix      Redis key prefix
     */
    public RedissonStore(RedissonClient redissonClient, String keyPrefix) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public void putItem(StoreItem item) {
        validatePutItem(item);
        try {
            String redisKey = createRedisKey(item.getNamespace(), item.getKey());
            String itemJson = objectMapper.writeValueAsString(item);
            // Directly use Redis atomic operation, no local lock required
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            bucket.set(itemJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store item in Redis via Redisson", e);
        }
    }

    @Override
    public Optional<StoreItem> getItem(List<String> namespace, String key) {
        validateGetItem(namespace, key);
        try {
            String redisKey = createRedisKey(namespace, key);
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            String value = bucket.get();
            if (value == null) {
                return Optional.empty();
            }
            StoreItem item = objectMapper.readValue(value, StoreItem.class);
            return Optional.of(item);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve item from Redis via Redisson", e);
        }
    }

    @Override
    public boolean deleteItem(List<String> namespace, String key) {
        validateDeleteItem(namespace, key);
        try {
            String redisKey = createRedisKey(namespace, key);
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            return bucket.delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete item from Redis via Redisson", e);
        }
    }

    @Override
    public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
        validateSearchItems(searchRequest);
        try {
            // 1. Use SCAN pattern to find matching keys efficiently (Avoid KEYS *)
            String pattern = keyPrefix + "*";
            List<StoreItem> allItems = getAllItemsByPattern(pattern);
            // 2. Apply filters in memory (same as original logic)
            List<StoreItem> filteredItems = allItems.stream()
                    .filter(item -> matchesSearchCriteria(item, searchRequest))
                    .collect(Collectors.toList());
            // 3. Sort items
            if (!searchRequest.getSortFields().isEmpty()) {
                filteredItems.sort(createComparator(searchRequest));
            }
            long totalCount = filteredItems.size();
            // 4. Apply pagination
            int offset = searchRequest.getOffset();
            int limit = searchRequest.getLimit();
            if (offset >= filteredItems.size()) {
                return StoreSearchResult.of(Collections.emptyList(), totalCount, offset, limit);
            }
            int endIndex = Math.min(offset + limit, filteredItems.size());
            List<StoreItem> resultItems = filteredItems.subList(offset, endIndex);
            return StoreSearchResult.of(resultItems, totalCount, offset, limit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search items in Redis via Redisson", e);
        }
    }

    @Override
    public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
        validateListNamespaces(namespaceRequest);
        try {
            String pattern = keyPrefix + "*";
            List<StoreItem> allItems = getAllItemsByPattern(pattern);
            Set<String> namespaceSet = new HashSet<>();
            List<String> prefixFilter = namespaceRequest.getNamespace();
            for (StoreItem item : allItems) {
                List<String> itemNamespace = item.getNamespace();
                // Check if namespace starts with prefix filter
                if (!prefixFilter.isEmpty() && !startsWithPrefix(itemNamespace, prefixFilter)) {
                    continue;
                }
                // Generate all possible namespace paths up to maxDepth
                int maxDepth = namespaceRequest.getMaxDepth();
                int depth = (maxDepth == -1) ? itemNamespace.size() : Math.min(maxDepth, itemNamespace.size());
                for (int i = 1; i <= depth; i++) {
                    String namespacePath = String.join("/", itemNamespace.subList(0, i));
                    namespaceSet.add(namespacePath);
                }
            }
            List<String> namespaces = new ArrayList<>(namespaceSet);
            Collections.sort(namespaces);
            // Apply pagination
            int offset = namespaceRequest.getOffset();
            int limit = namespaceRequest.getLimit();
            if (offset >= namespaces.size()) {
                return Collections.emptyList();
            }
            int endIndex = Math.min(offset + limit, namespaces.size());
            return namespaces.subList(offset, endIndex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list namespaces in Redis via Redisson", e);
        }
    }

    @Override
    public void clear() {
        try {
            String pattern = keyPrefix + "*";
            Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);
            for (String key : keys) {
                redissonClient.getKeys().delete(key);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear Redis store via Redisson", e);
        }
    }

    @Override
    public long size() {
        try {
            String pattern = keyPrefix + "*";
            long count = 0;
            // Use iterator to avoid O(N) memory blowup
            Iterator<String> iterator = redissonClient.getKeys().getKeysByPattern(pattern).iterator();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get size from Redis via Redisson", e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            String pattern = keyPrefix + "*";
            Iterator<String> iterator = redissonClient.getKeys().getKeysByPattern(pattern).iterator();
            return !iterator.hasNext();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check isEmpty from Redis via Redisson", e);
        }
    }

    /**
     * Create Redis key from namespace and key.
     *
     * @param namespace namespace
     * @param key       key
     * @return Redis key
     */
    private String createRedisKey(List<String> namespace, String key) {
        String storeKey = createStoreKey(namespace, key);
        return keyPrefix + storeKey;
    }

    /**
     * Retrieve all items matching a specific pattern using Redis SCAN.
     * Uses RBatch for bulk fetching to optimize network round-trips.
     *
     * @param pattern key pattern
     * @return list of matching StoreItems
     */
    private List<StoreItem> getAllItemsByPattern(String pattern) {
        List<StoreItem> items = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        // 1. Collect keys using non-blocking SCAN
        redissonClient.getKeys().getKeysByPattern(pattern).forEach(keys::add);
        if (keys.isEmpty()) {
            return items;
        }
        // 2. Batch get values to optimize performance
        try {
            RBatch batch = redissonClient.createBatch();
            Map<String, RFuture<String>> futures = new HashMap<>();
            for (String key : keys) {
                futures.put(key, batch.<String>getBucket(key).getAsync());
            }
            batch.execute();
            // 3. Deserialize results
            for (Map.Entry<String, RFuture<String>> entry : futures.entrySet()) {
                try {
                    String value = entry.getValue().getNow();
                    if (value != null) {
                        StoreItem item = objectMapper.readValue(value, StoreItem.class);
                        items.add(item);
                    }
                } catch (Exception e) {
                    // Skip invalid items
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Batch fetching items failed", e);
        }
        return items;
    }
}