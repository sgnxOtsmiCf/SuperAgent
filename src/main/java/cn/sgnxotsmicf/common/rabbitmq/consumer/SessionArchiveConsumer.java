package cn.sgnxotsmicf.common.rabbitmq.consumer;

import cn.sgnxotsmicf.common.dto.ArchiveMessage;
import cn.sgnxotsmicf.common.rabbitmq.constant.MqConst;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import cn.sgnxotsmicf.service.SessionArchiveService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 会话归档消息消费者
 *
 * 监听归档队列，处理会话归档逻辑
 * 使用手动ACK确认机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionArchiveConsumer {

    private final SessionArchiveService sessionArchiveService;

    private final RabbitService rabbitService;

    /**
     * 消费归档消息
     *
     * @param archiveMessage 归档消息
     * @param channel        RabbitMQ Channel
     * @param message        AMQP Message
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = MqConst.QUEUE_ARCHIVE_SESSION,
                            durable = "true",
                            arguments = {
                                    @org.springframework.amqp.rabbit.annotation.Argument(
                                            name = "x-dead-letter-exchange",
                                            value = MqConst.EXCHANGE_ARCHIVE_DLX
                                    ),
                                    @org.springframework.amqp.rabbit.annotation.Argument(
                                            name = "x-dead-letter-routing-key",
                                            value = MqConst.ROUTING_ARCHIVE_DLX
                                    ),
                                    @org.springframework.amqp.rabbit.annotation.Argument(
                                            name = "x-message-ttl",
                                            value = "86400000",
                                            type = "java.lang.Integer"
                                    )
                            }
                    ),
                    exchange = @Exchange(value = MqConst.EXCHANGE_ARCHIVE, durable = "true"),
                    key = MqConst.ROUTING_ARCHIVE_SESSION
            ),
            ackMode = "MANUAL"
    )
    public void consumeArchiveMessage(ArchiveMessage archiveMessage,
                                       Channel channel,
                                       Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String sessionId = archiveMessage.getSessionId();

        log.info("收到归档消息: messageId={}, sessionId={}, triggerType={}",
                archiveMessage.getMessageId(), sessionId, archiveMessage.getTriggerType());

        try {
            // 幂等性检查
            if (sessionArchiveService.isSessionArchived(sessionId)) {
                log.info("会话已归档，跳过处理: sessionId={}", sessionId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 执行归档
            boolean success = sessionArchiveService.processArchiveMessage(archiveMessage);

            if (success) {
                // 处理成功，手动确认
                channel.basicAck(deliveryTag, false);
                log.info("归档消息处理成功并已确认: sessionId={}", sessionId);
            } else {
                // 处理失败，增加重试次数并重新发送
                Integer retryCount = archiveMessage.getRetryCount();
                if (retryCount != null && retryCount >= 3) {
                    log.error("归档消息重试次数超过限制，进入死信队列: sessionId={}", sessionId);
                    channel.basicNack(deliveryTag, false, false); // 不重新入队，进入死信队列
                } else {
                    // 增加重试次数并重新发送新消息
                    archiveMessage.incrementRetry();
                    log.warn("归档处理失败，增加重试次数后重新发送: sessionId={}, retryCount={}",
                            sessionId, archiveMessage.getRetryCount());

                    // 发送增加重试次数后的新消息
                    rabbitService.sendMessage(
                            MqConst.EXCHANGE_ARCHIVE,
                            MqConst.ROUTING_ARCHIVE_SESSION,
                            archiveMessage
                    );

                    // 确认原消息（避免重复消费）
                    channel.basicAck(deliveryTag, false);
                }
            }
        } catch (Exception e) {
            log.error("归档消息处理异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
            // 异常时进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 死信队列消费者 - 处理失败的消息
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = MqConst.QUEUE_ARCHIVE_DLX, durable = "true"),
                    exchange = @Exchange(value = MqConst.EXCHANGE_ARCHIVE_DLX, durable = "true"),
                    key = MqConst.ROUTING_ARCHIVE_DLX
            ),
            ackMode = "MANUAL"
    )
    public void consumeDeadLetterMessage(ArchiveMessage archiveMessage,
                                          Channel channel,
                                          Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String sessionId = archiveMessage.getSessionId();

        log.error("收到死信消息: sessionId={}, messageId={}", sessionId, archiveMessage.getMessageId());

        // TODO: 将失败消息存入数据库，人工处理
        // 发送告警通知等

        // 确认消息
        channel.basicAck(deliveryTag, false);
    }
}
