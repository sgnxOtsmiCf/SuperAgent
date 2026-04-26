package cn.sgnxotsmicf.common.rabbitmq.entity;

import cn.sgnxotsmicf.common.po.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/26 16:05
 * @Version: 1.0
 * @Description: 消息发送失败记录类
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("superagent_mq_fail_message")
public class MqFailMessage extends BaseEntity {

    /** 消息唯一标识（CorrelationData ID） */
    private String messageId;

    /** 交换机名称 */
    private String exchange;

    /** 路由键 */
    private String routingKey;

    /** 消息体（JSON字符串） */
    private String messageBody;

    /** 失败原因 */
    private String failReason;

    /** 已重试次数 */
    private Integer retryCount;

    /** 状态：0-待处理 1-已处理 2-已忽略 */
    private Integer status;

    /** 处理备注（人工填写） */
    private String handlerRemark;


}
