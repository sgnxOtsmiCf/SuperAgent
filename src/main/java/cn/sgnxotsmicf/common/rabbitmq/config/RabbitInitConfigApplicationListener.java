package cn.sgnxotsmicf.common.rabbitmq.config;

import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.common.rabbitmq.entity.MqFailMessage;
import cn.sgnxotsmicf.common.rabbitmq.entity.SuperAgentCorrelationData;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 初始化配置监听器
 * <p>
 * 规范：
 * 1. ConfirmCallback（Exchange 不可达）：退避重试 3 次，仍失败则入库告警
 * 2. ReturnsCallback（路由到队列失败）：不再重试，直接入库告警（配置错误不会自动恢复）
 * 3. 延迟消息在 ReturnsCallback 中不再重试，避免延迟时间累积
 */
@Slf4j
@Component
public class RabbitInitConfigApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource(name = "superAgentRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final RabbitService rabbitService;

    public RabbitInitConfigApplicationListener(StringRedisTemplate stringRedisTemplate, RabbitService rabbitService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitService = rabbitService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.setupCallbacks();
    }

    /**
     * 设置回调函数
     */
    private void setupCallbacks() {
        // Exchange 确认回调
        this.rabbitTemplate.setConfirmCallback(this::handleConfirm);
        // 路由失败回调
        this.rabbitTemplate.setReturnsCallback(this::handleReturn);
    }

    /**
     * 消息发送到 Exchange 的确认回调
     */
    private void handleConfirm(CorrelationData correlationData, boolean ack, String reason) {
        if (!(correlationData instanceof SuperAgentCorrelationData)) {
            if (!ack) {
                log.error("消息发送到Exchange失败，且无法识别的CorrelationData类型: {}", correlationData);
            }
            return;
        }

        SuperAgentCorrelationData data = (SuperAgentCorrelationData) correlationData;

        if (ack) {
            log.debug("消息发送到Exchange成功: {}", data.getId());
            // 成功即清理 Redis，防止数据堆积
            stringRedisTemplate.delete(data.getId());
            return;
        }

        log.error("消息发送到Exchange失败: id={}, reason={}", data.getId(), reason);
        // Exchange 不可达，进行有限重试
        this.retrySend(data);
    }



    /**
     * 消息从 Exchange 路由到 Queue 失败的回调
     * <p>
     * 注意：此时消息已到达 Exchange，只是没进队列（配置错误、队列未创建等）。
     * 这类错误不会自动恢复，因此企业级规范是：直接记录失败，不再重试。
     */
    private void handleReturn(ReturnedMessage returned) {
        log.error("消息路由到队列失败: replyCode={}, replyText={}, exchange={}, routingKey={}",
                returned.getReplyCode(),
                returned.getReplyText(),
                returned.getExchange(),
                returned.getRoutingKey());

        // 获取关联Id(优先 Spring 内部 header，fallback 到 correlationId)
        String redisKey = returned.getMessage().getMessageProperties()
                .getHeader("spring_returned_message_correlation");
        if (redisKey == null) {
            redisKey = returned.getMessage().getMessageProperties().getCorrelationId();
        }

        if (redisKey == null) {
            log.error("无法获取消息关联ID，body={}", new String(returned.getMessage().getBody()));
            return;
        }

        String correlationDataStr = stringRedisTemplate.opsForValue().get(redisKey);
        if (correlationDataStr == null) {
            log.error("Redis中未找到关联数据，key={}，可能已超时或已被清理", redisKey);
            return;
        }

        SuperAgentCorrelationData data = JSONUtil.toBean(correlationDataStr, SuperAgentCorrelationData.class);

        String failReason = String.format("路由到队列失败: replyCode=%d, replyText=%s",
                returned.getReplyCode(), returned.getReplyText());

        if (data.isDelay()) {
            log.error("延迟消息路由失败，不再重试（避免延迟时间累积）: exchange={}, routingKey={}, delayTime={}ms",
                    data.getExchange(), data.getRoutingKey(), data.getDelayTime());
        } else {
            log.error("普通消息路由失败，不再重试（配置错误不会自动恢复）: exchange={}, routingKey={}",
                    data.getExchange(), data.getRoutingKey());
        }

        this.saveFailMessage(data, failReason);
        // 清理 Redis
        stringRedisTemplate.delete(redisKey);
    }

    /**
     * 重发消息（仅用于 Exchange 确认失败场景）
     */
    private void retrySend(SuperAgentCorrelationData data) {
        int retryCount = data.getRetryCount();

        if (retryCount >= 3) {
            log.error("生产者超过最大重试次数，消息发送失败: exchange={}, routingKey={}",
                    data.getExchange(), data.getRoutingKey());
            this.saveFailMessage(data, "发送到Exchange失败，超过最大重试次数");
            stringRedisTemplate.delete(data.getId());
            return;
        }

        retryCount++;
        data.setRetryCount(retryCount);
        stringRedisTemplate.opsForValue().set(data.getId(), JSONUtil.toJsonStr(data), 10, TimeUnit.MINUTES);

        log.info("进行消息重发: retryCount={}, exchange={}, routingKey={}",
                retryCount, data.getExchange(), data.getRoutingKey());

        // 简单退避：第1次等1秒，第2次等2秒，第3次等3秒
        try {
            Thread.sleep(retryCount * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("重发退避被中断，取消本次重发");
            return;
        }

        if (data.isDelay()) {
            rabbitService.sendDelayMessage(
                    data.getExchange(),
                    data.getRoutingKey(),
                    data.getMessage(),
                    data.getDelayTime()
            );
        } else {
            rabbitService.sendMessage(
                    data.getExchange(),
                    data.getRoutingKey(),
                    data.getMessage()
            );
        }
    }


    /**
     * 保存失败消息
     * <p>
     * 通过定时任务或管理后台人工处理/重新发送
     */
    private void saveFailMessage(SuperAgentCorrelationData data, String reason) {
        MqFailMessage failMsg = new MqFailMessage();
        failMsg.setMessageId(data.getId());
        failMsg.setExchange(data.getExchange());
        failMsg.setRoutingKey(data.getRoutingKey());
        failMsg.setMessageBody((String) data.getMessage());
        failMsg.setFailReason(reason);
        failMsg.setRetryCount(data.getRetryCount());
        failMsg.setStatus(0); // 待处理
        log.error("【消息失败待处理】id={}, exchange={}, routingKey={}, reason={}, message={}",
                data.getId(), data.getExchange(), data.getRoutingKey(), reason, data.getMessage());
    }


}
