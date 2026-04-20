package cn.sgnxotsmicf.common.rabbitmq.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.sgnxotsmicf.common.dto.ChatSessionDTO;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.rabbitmq.constant.MqConst;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

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
                                    value = "86400000",
                                    type = "java.lang.Integer"
                            )
                    }
            ),
            exchange = @Exchange(value = MqConst.EXCHANGE_CHAT_SESSION_DB, durable = "true"),
            key = MqConst.ROUTING_CHAT_SESSION_DB
    ), ackMode = "MANUAL")
    public void consumeArchiveMessage(ChatSessionDTO chatSessionDTO, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String sessionId = chatSessionDTO.getSessionId();
        log.info("收到会话消息: sessionId={}, triggerType={}", sessionId, "auto");
        ChatSession chatSession = new ChatSession();
        BeanUtil.copyProperties(chatSessionDTO, chatSession);
        int flag = 0;
        if (chatSessionDTO.getDoType().equals(ChatSessionDTO.INSERT)){
            flag = chatSessionMapper.insert(chatSession);
        }else if (chatSessionDTO.getDoType().equals(ChatSessionDTO.UPDATE)){
            flag = chatSessionMapper.update(new LambdaUpdateWrapper<ChatSession>()
                    .set(ChatSession::getLastActive, LocalDateTime.now())
                    .eq(ChatSession::getSessionId, sessionId));
        }
        if (flag > 0){
            // 处理成功，手动确认
            channel.basicAck(deliveryTag, false);
            log.info("会话消息处理成功并已确认: sessionId={}", sessionId);
        } else {
            // 处理失败，增加重试次数并重新发送
            Integer retryCount = chatSessionDTO.getRetryCount();
            if (retryCount != null && retryCount >= 3) {
                log.error("会话消息重试次数超过限制，进入死信队列: sessionId={}", sessionId);
                channel.basicNack(deliveryTag, false, false); // 不重新入队，进入死信队列
            } else {
                // 增加重试次数并重新发送新消息
                chatSessionDTO.incrementRetry();
                log.warn("会话消息处理失败，增加重试次数后重新发送: sessionId={}, retryCount={}",
                        sessionId, chatSessionDTO.getRetryCount());

                // 发送增加重试次数后的新消息
                rabbitService.sendMessage(
                        MqConst.EXCHANGE_ARCHIVE,
                        MqConst.ROUTING_ARCHIVE_SESSION,
                        chatSessionDTO
                );
                // 确认原消息（避免重复消费）
                channel.basicAck(deliveryTag, false);
            }
        }

    }
}
