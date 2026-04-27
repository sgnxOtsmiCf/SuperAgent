package cn.sgnxotsmicf.common.agent;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/7 15:15
 * @Version: 1.0
 * @Description: Agent 通用常量配置
 */

public interface AgentCommon {

    // ==================== Redis Key 前缀 ====================
    String RedisFamilyHarmonyKeyPrefix = "familyHarmony:";
    String RedisManusKeyPrefix = "manus:";
    String RedisSuperAgentKeyPrefix = "superagent:";
    String RedisAgentSessionKeyPrefix = "AgentSession:";
    // ==================== Agent 类型标识 ====================
    String OpenManus = "openManus";
    String FamilyHarmony = "familyHarmony";
    String SuperAgent = "SuperAgent";
    // ==================== Agent ID 定义====================
    Long SuperAgentId = 1L;
    Long OpenManusId = 2L;
    Long FamilyHarmonyId = 3L;

}
