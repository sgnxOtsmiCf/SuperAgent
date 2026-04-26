package cn.sgnxotsmicf.common.rabbitmq.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.rabbitmq.constant.MqConst;
import cn.sgnxotsmicf.common.rabbitmq.entity.MqFailMessage;
import cn.sgnxotsmicf.common.rabbitmq.entity.SessionMessage;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
import cn.sgnxotsmicf.dao.MqFailMessageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/18 14:12
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionConsumer {

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private RabbitService rabbitService;

    @Resource
    private MqFailMessageMapper mqFailMessageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = MqConst.QUEUE_CHAT_SESSION_DB,
                    durable = "true",
                    arguments = {
                            @org.springframework.amqp.rabbit.annotation.Argument(
                                    name = "x-dead-letter-exchange",
                                    value = MqConst.EXCHANGE_CHAT_SESSION_DB_DLX
                            ),
                            @org.springframework.amqp.rabbit.annotation.Argument(
                                    name = "x-dead-letter-routing-key",
                                    value = MqConst.ROUTING_CHAT_SESSION_DB_DLX
                            ),
                            @org.springframework.amqp.rabbit.annotation.Argument(
                                    name = "x-message-ttl",
                                    value = "3600000",
                                    type = "java.lang.Integer"
                            )
                    }
            ),
            exchange = @Exchange(value = MqConst.EXCHANGE_CHAT_SESSION_DB, durable = "true"),
            key = MqConst.ROUTING_CHAT_SESSION_DB
    ), ackMode = "MANUAL")
    public void consumeArchiveMessage(SessionMessage sessionMessage, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String sessionId = sessionMessage.getSessionId();
        // 消费前先判断Redis锁,做幂等处理
        String lockKey = "mq:lock:" + sessionMessage.getMessageId();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lock)) {
            log.info("消息已消费，跳过: sessionId={}", sessionId);
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            log.info("收到会话消息: sessionId={}, triggerType={}", sessionId, "auto");
            int flag = handleBusiness(sessionMessage);
            if (flag > 0) {
                // 处理成功，手动确认
                channel.basicAck(deliveryTag, false);
                log.info("会话消息处理成功并已确认: sessionId={}", sessionId);
            } else {
                // 处理失败，增加重试次数并重新发送
                Integer retryCount = sessionMessage.getRetryCount();
                if (retryCount != null && retryCount >= 3) {
                    log.error("会话消息重试次数超过限制，进入死信队列: sessionId={}", sessionId);
                    channel.basicNack(deliveryTag, false, false); // 不重新入队，进入死信队列
                } else {
                    // 增加重试次数并重新发送新消息
                    sessionMessage.incrementRetry();
                    log.warn("会话消息处理失败，增加重试次数后重新发送: sessionId={}, retryCount={}",
                            sessionId, sessionMessage.getRetryCount());

                    // 发送增加重试次数后的新消息
                    rabbitService.sendMessage(
                            MqConst.EXCHANGE_CHAT_SESSION_DB,
                            MqConst.ROUTING_CHAT_SESSION_DB,
                            sessionMessage
                    );
                    // 确认原消息（避免重复消费）
                    channel.basicAck(deliveryTag, false);
                }
            }
        } catch (Exception e) {
            log.error("消息处理失败: sessionId={}", sessionId, e);
            // 3. 执行失败：拒绝消息，Spring重试耗尽后 → 自动进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }


    /**
     * 死信队列消费者 - 处理失败的消息
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = MqConst.QUEUE_CHAT_SESSION_DB_DLX, durable = "true"),
                    exchange = @Exchange(value = MqConst.EXCHANGE_CHAT_SESSION_DB_DLX, durable = "true"),
                    key = MqConst.ROUTING_CHAT_SESSION_DB_DLX
            ),
            ackMode = "MANUAL"
    )
    public void consumeDeadLetterMessage(SessionMessage sessionMessage, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String sessionId = sessionMessage.getSessionId();

        log.error("收到死信消息: sessionId={}, RetryCount={}", sessionId, sessionMessage.getRetryCount());

        //死信消息永久入库
        try {
            MqFailMessage failMsg = new MqFailMessage();
            failMsg.setMessageId(sessionMessage.getMessageId());
            failMsg.setExchange(MqConst.EXCHANGE_CHAT_SESSION_DB);
            failMsg.setRoutingKey(MqConst.ROUTING_CHAT_SESSION_DB);
            failMsg.setMessageBody(JSONUtil.toJsonStr(sessionMessage));
            failMsg.setFailReason("消费端处理失败进入死信: " +
                    (sessionMessage.getRetryCount() >= 3 ? "超过最大重试次数" : "业务/系统异常"));
            failMsg.setRetryCount(sessionMessage.getRetryCount());
            failMsg.setStatus(0);
            failMsg.setCreateTime(LocalDateTime.now());
            failMsg.setUpdateTime(LocalDateTime.now());
            mqFailMessageMapper.insert(failMsg);
            channel.basicAck(deliveryTag, false);
            log.info("死信消息已入库并确认: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("死信消息入库失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            // 入库失败，NACK 不重新入队。TODO:需要额外处理
            channel.basicNack(deliveryTag, false, false);
        }
    }



    private int handleBusiness(SessionMessage sessionMessage) {
        ChatSession chatSession = new ChatSession();
        BeanUtil.copyProperties(sessionMessage, chatSession);
        if (SessionMessage.INSERT.equals(sessionMessage.getDoType())) {
            return chatSessionMapper.insert(chatSession);
        } else if (SessionMessage.UPDATE.equals(sessionMessage.getDoType())) {
            return chatSessionMapper.update(new LambdaUpdateWrapper<ChatSession>()
                    .set(ChatSession::getLastActive, LocalDateTime.now())
                    .eq(ChatSession::getSessionId, sessionMessage.getSessionId()));
        }
        return 0;
    }
}
