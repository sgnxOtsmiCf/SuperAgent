package cn.sgnxotsmicf.common.rabbitmq.constant;

/**
 * RabbitMQ 常量定义
 * 
 * 归档相关队列、交换机、路由键定义
 */
public class MqConst {

    /**
     * 会话归档交换机
     */
    public static final String EXCHANGE_ARCHIVE = "superagent.archive";
    
    /**
     * 会话归档路由键
     */
    public static final String ROUTING_ARCHIVE_SESSION = "superagent.archive.session";
    
    /**
     * 会话归档队列
     */
    public static final String QUEUE_ARCHIVE_SESSION = "superagent.archive.session";
    
    /**
     * 会话归档死信交换机
     */
    public static final String EXCHANGE_ARCHIVE_DLX = "superagent.archive.dlx";
    
    /**
     * 会话归档死信路由键
     */
    public static final String ROUTING_ARCHIVE_DLX = "superagent.archive.dlx";
    
    /**
     * 会话归档死信队列
     */
    public static final String QUEUE_ARCHIVE_DLX = "superagent.archive.dlx";

    /**
     * 会话数据库交互交换机
     */
    public static final String EXCHANGE_CHAT_SESSION_DB = "superagent.chat.session.db";

    /**
     * 会话数据库交互队列
     */
    public static final String QUEUE_CHAT_SESSION_DB = "superagent.chat.session.db";

    /**
     * 会话数据库交互路由键
     */
    public static final String ROUTING_CHAT_SESSION_DB = "superagent.chat.session.db";

    /**
     * 会话数据库交互死信交换机
     */
    public static final String EXCHANGE_CHAT_SESSION_DB_DLX = "superagent.chat.session.db.dlx";

    /**
     * 会话数据库交互死信队列
     */
    public static final String QUEUE_CHAT_SESSION_DB_DLX = "superagent.chat.session.db.dlx";

    /**
     * 会话数据库交互死信路由键
     */
    public static final String ROUTING_CHAT_SESSION_DB_DLX = "superagent.chat.session.db.dlx";

    
    /**
     * 消息TTL 24小时（毫秒）
     */
    public static final Integer MESSAGE_TTL = 24 * 60 * 60 * 1000;
}
