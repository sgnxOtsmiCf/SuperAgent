package cn.sgnxotsmicf.common.rabbitmq.config;

import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.common.rabbitmq.entity.SuperAgentCorrelationData;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 初始化配置监听器
 * <p>
 * 应用启动后设置消息确认回调
 * 处理生产者端的消息确认和重试
 */
@Slf4j
@Component
public class RabbitInitConfigApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource(name = "superAgentRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.setupCallbacks();
    }

    /**
     * 设置回调函数
     */
    private void setupCallbacks() {
        /**
         * 消息发送到Exchange的确认回调
         * ack=true: 消息成功到达Exchange
         * ack=false: 消息到达Exchange失败
         */
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, reason) -> {
            if (ack) {
                log.debug("消息发送到Exchange成功: {}", correlationData);
            } else {
                log.error("消息发送到Exchange失败: {}, reason: {}", correlationData, reason);
                // 执行消息重发
                this.retrySendMsg(correlationData);
            }
        });

        /**
         * 消息路由到队列失败的回调
         * 只有消息没有正确到达队列时才会触发
         */
        this.rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由到队列失败: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getMessage(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());

            // 从Redis获取相关数据
            String redisKey = returned.getMessage().getMessageProperties().getHeader("spring_returned_message_correlation");
            String correlationDataStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (correlationDataStr != null) {
                SuperAgentCorrelationData correlationData = JSONUtil.toBean(correlationDataStr, SuperAgentCorrelationData.class);
                // 延迟消息不重发
                if (!correlationData.isDelay()) {
                    this.retrySendMsg(correlationData);
                }
            }
        });
    }

    /**
     * 消息重新发送
     *
     * @param correlationData 相关数据
     */
    private void retrySendMsg(CorrelationData correlationData) {
        if (!(correlationData instanceof SuperAgentCorrelationData)) {
            log.error("无法识别的CorrelationData类型");
            return;
        }

        SuperAgentCorrelationData superAgentData = (SuperAgentCorrelationData) correlationData;
        int retryCount = superAgentData.getRetryCount();

        // 超过最大重试次数
        if (retryCount >= 3) {
            log.error("生产者超过最大重试次数，消息发送失败: exchange={}, routingKey={}",
                    superAgentData.getExchange(), superAgentData.getRoutingKey());
            // TODO: 将失败消息存入数据库，人工处理
            return;
        }

        // 重试次数+1
        retryCount++;
        superAgentData.setRetryCount(retryCount);

        // 更新Redis
        String redisKey = superAgentData.getId();
        stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(superAgentData), 10, TimeUnit.MINUTES);

        log.info("进行消息重发: retryCount={}, exchange={}, routingKey={}",
                retryCount, superAgentData.getExchange(), superAgentData.getRoutingKey());

        // 重发消息
        if (superAgentData.isDelay()) {
            // 延迟消息
            rabbitService.sendDelayMessage(
                    superAgentData.getExchange(),
                    superAgentData.getRoutingKey(),
                    superAgentData.getMessage(),
                    superAgentData.getDelayTime()
            );
        } else {
            // 普通消息
            rabbitService.sendMessage(
                    superAgentData.getExchange(),
                    superAgentData.getRoutingKey(),
                    superAgentData.getMessage()
            );
        }
    }
}
