package cn.sgnxotsmicf.common.rabbitmq.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * 自定义相关数据对象
 * 
 * 包含消息重试机制、延迟消息支持
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SuperAgentCorrelationData extends CorrelationData {

    /**
     * 消息体
     */
    private Object message;
    
    /**
     * 交换机
     */
    private String exchange;
    
    /**
     * 路由键
     */
    private String routingKey;
    
    /**
     * 重试次数
     */
    private int retryCount = 0;
    
    /**
     * 是否延迟消息
     */
    private boolean isDelay = false;
    
    /**
     * 延迟时长（秒）
     */
    private int delayTime = 10;
}
