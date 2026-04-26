package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.rabbitmq.entity.ArchiveMessage;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;

import java.util.Collection;
import java.util.List;

/**
 * 会话归档服务接口
 * <p>
 * 符合现有Service接口规范
 * 提供会话归档相关的业务逻辑
 */
public interface SessionArchiveService {

    /**
     * 发送归档消息到队列（手动归档）
     *
     * @param sessionId 会话ID
     * @param agentId   Agent ID
     * @return 操作结果
     */
    Result<String> sendArchiveMessage(String sessionId, Long agentId);

    /**
     * 发送归档消息到队列（自动归档）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param agentId   Agent ID
     * @return 操作结果
     */
    Result<String> sendAutoArchiveMessage(String sessionId, Long userId, Long agentId);

    /**
     * 批量发送归档消息
     *
     * @param messages 归档消息列表
     * @return 操作结果
     */
    Result<String> sendBatchArchiveMessages(List<ArchiveMessage> messages);


    /**
     * 批量发送归档消息--通过sessionId
     *
     * @param sessionList 归档消息列表
     * @return 操作结果
     */
    Result<String> sendBatchArchiveMessagesBySessionIdList(Collection<String> sessionList, Long userId, Long agentId);

    /**
     * 处理归档消息（消费者调用）
     *
     * @param message 归档消息
     * @return 是否处理成功
     */
    boolean processArchiveMessage(ArchiveMessage message);

    /**
     * 检查会话是否已归档
     *
     * @param sessionId 会话ID
     * @return 是否已归档
     */
    boolean isSessionArchived(String sessionId);

    /**
     * 校验会话归属
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 是否属于该用户
     */
    boolean validateSessionOwnership(String sessionId, Long userId);

    /**
     * 获取已过期的会话列表
     *
     * @return 过期会话ID列表
     */
    List<String> getExpiredSessions();

    /**
     * 获取即将过期的会话列表
     *
     * @param days 多少天内过期
     * @return 即将过期会话ID列表
     */
    List<String> getExpiringSessions(int days);

    List<ChatSessionVo> getArchiveSessionPage(Long agentId, Integer pageNo, Integer pageSize);
}
