package cn.sgnxotsmicf.database;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 14:32
 * @Version: 1.0
 * @Description:
 */

@SpringBootTest
public class redisTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redisson;

    @Test
    public void test() {
        stringRedisTemplate.opsForValue().set("name","lixiang");
        String name = stringRedisTemplate.opsForValue().get("name");
        Assertions.assertEquals("lixiang",name);
        System.out.println("====================");
    }

    @Test
    public void redissonTest() {
        RBucket<Object> bucket1 = redisson.getBucket("key");
        bucket1.set("helloWorld");
        RBucket<String> bucket2 = redisson.getBucket("key");
        System.out.println(bucket2.get());
    }
}
