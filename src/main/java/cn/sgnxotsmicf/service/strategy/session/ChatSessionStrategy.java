package cn.sgnxotsmicf.service.strategy.session;

import cn.sgnxotsmicf.common.vo.ChatSessionVo;

import java.util.List;

/**
 * 聊天会话策略接口
 * <p>
 * 定义不同 Agent 类型下会话管理的标准操作。
 * 每种 Agent 实现类如 Manus、SuperAgent等需实现此接口，
 * 以提供各自特定的会话存储、查询和删除逻辑。
 * </p>
 *
 * @author sgnxotsmicf
 * @since 2026-04-17
 */
public interface ChatSessionStrategy {

    /**
     * 判断是否支持指定的 Agent 类型
     * <p>
     * 用于策略上下文中路由到正确的策略实现。
     * 例如：DeepSeekStrategy 返回 agentId == 1
     * </p>
     *
     * @param agentId Agent 类型标识符
     * @return true 表示当前策略支持该 Agent 类型，false 表示不支持
     */
    boolean supports(Long agentId);

    /**
     * 获取指定用户的全部会话列表
     * <p>
     * 适用于数据量较小的场景（如本地缓存、个人历史会话）。
     * 返回的列表通常按时间倒序排列。
     * </p>
     *
     * @return 会话视图对象列表，无数据时返回空列表（非null）
     */
    List<ChatSessionVo> getSessions(ChatSessionContext chatSessionContext);

    /**
     * 分页获取指定用户的会话列表
     * <p>
     * 适用于数据量较大的场景（如远程查询、数据库分页）。
     * </p>
     *
     * @return 当前页的会话视图对象列表
     */
    List<ChatSessionVo> getSessionsPage(ChatSessionContext chatSessionContext);

    /**
     * 根据会话ID获取单条会话详情
     * <p>
     * 用于进入具体聊天窗口时加载会话上下文。
     * </p>
     *
     * @return 会话视图对象；若不存在或无权限则返回 null 或抛出业务异常（由实现类决定）
     */
    ChatSessionVo getSessionBySessionId(ChatSessionContext chatSessionContext);

    /**
     * 删除 Memory 中的指定会话
     * <p>
     * 仅删除内存/缓存中的会话数据，不影响持久化存储。
     * 常用于清理过期会话或释放内存资源。
     * </p>
     *
     * @return true 删除成功，false 删除失败或会话不存在
     */
    boolean deleteMemorySessionById(ChatSessionContext chatSessionContext);


    /**
     * 获取指定用户的指定agent的会话总记录数
     * <p>
     * 用于前端用户数据详细展示。
     * </p>
     *
     * @return 会话总记录数，无数据时返回0
     */
    long getSessionCount(ChatSessionContext chatSessionContext);
}