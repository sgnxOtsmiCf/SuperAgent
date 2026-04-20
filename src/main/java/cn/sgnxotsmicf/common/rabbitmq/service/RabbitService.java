package cn.sgnxotsmicf.common.rabbitmq.service;

import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.common.rabbitmq.entity.SuperAgentCorrelationData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 消息发送服务
 * <p>
 * 提供消息发送、延迟消息发送功能
 * 支持消息重试机制
 */
@Slf4j
@Service
public class RabbitService {

    @Resource(name = "superAgentRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @return 是否发送成功
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        // 1. 创建自定义相关消息对象
        SuperAgentCorrelationData correlationData = new SuperAgentCorrelationData();
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(uuid);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);

        // 2. 将相关消息存入Redis，10分钟过期
        stringRedisTemplate.opsForValue().set(uuid, JSONUtil.toJsonStr(correlationData), 10, TimeUnit.MINUTES);

        // 3. 发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
        log.debug("消息已发送: exchange={}, routingKey={}", exchange, routingKey);
        return true;
    }

    /**
     * 发送延迟消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息数据
     * @param delayTime  延迟时间（秒）
     * @return 是否发送成功
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        // 1. 创建自定义相关消息对象
        SuperAgentCorrelationData correlationData = new SuperAgentCorrelationData();
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(uuid);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        correlationData.setDelay(true);
        correlationData.setDelayTime(delayTime);

        // 2. 将相关消息存入Redis
        stringRedisTemplate.opsForValue().set(uuid, JSONUtil.toJsonStr(correlationData), 10, TimeUnit.MINUTES);

        // 3. 发送延迟消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setDelayLong(delayTime * 1000L);
            return msg;
        }, correlationData);
        log.debug("延迟消息已发送: exchange={}, routingKey={}, delayTime={}s", exchange, routingKey, delayTime);
        return true;
    }
}
