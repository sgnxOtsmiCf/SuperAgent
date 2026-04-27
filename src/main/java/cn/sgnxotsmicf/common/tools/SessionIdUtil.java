package cn.sgnxotsmicf.common.tools;

import cn.sgnxotsmicf.common.agent.AgentCommon;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32;
import java.util.stream.Collectors;

/**
 * SessionId生成与校验工具类
 *
 * <p>企业级SessionId规范：前缀(2) + 时间戳(8) + 随机数(6) + 校验码(4) = 20字符</p>
 *
 * <p>核心能力：</p>
 * <ul>
 *   <li>支持多Agent类型隔离（SuperAgent/OpenManus/FamilyHarmony）</li>
 *   <li>防篡改校验（CRC32校验码机制）</li>
 *   <li>有效期控制（默认30天）</li>
 *   <li>双重校验：SessionId合法性 + AgentId一致性</li>
 *   <li>批量校验支持（Set/List集合高效处理）</li>
 * </ul>
 *
 * <p>线程安全：本类为无状态工具类，所有方法均为线程安全</p>
 *
 * @author lixiang
 * @version 2.2
 * @since 2026-04-11
 */
@Slf4j
public class SessionIdUtil {

    // ==================== 常量定义 ====================

    /**
     * 字符集：62进制 (0-9, A-Z, a-z)
     */
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int BASE62_LENGTH = BASE62.length();

    /**
     * 前缀长度（2字符）
     */
    private static final int PREFIX_LENGTH = 2;

    /**
     * 时间戳长度（36进制，8字符）
     */
    private static final int TIMESTAMP_LENGTH = 8;

    /**
     * 随机数长度（62进制，6字符）
     */
    private static final int RANDOM_LENGTH = 6;

    /**
     * 校验码长度（CRC32取前4位，4字符）
     */
    private static final int CHECKSUM_LENGTH = 4;

    /**
     * 标准SessionId总长度（20字符）
     */
    private static final int TOTAL_LENGTH = PREFIX_LENGTH + TIMESTAMP_LENGTH + RANDOM_LENGTH + CHECKSUM_LENGTH;

    /**
     * 最大允许长度（数据库varchar(32)限制）
     */
    private static final int MAX_LENGTH = 32;

    /**
     * 安全随机数生成器（线程安全）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ==================== Agent映射配置 ====================

    /**
     * 前缀与Agent类型映射
     */
    private static final Map<String, String> PREFIX_TO_AGENT_TYPE = new HashMap<>();

    /**
     * 前缀与AgentId映射
     */
    private static final Map<String, Long> PREFIX_TO_AGENT_ID = new HashMap<>();

    /**
     * Agent类型与前缀映射
     */
    private static final Map<String, String> AGENT_TYPE_TO_PREFIX = new HashMap<>();

    /**
     * AgentId与前缀映射
     */
    private static final Map<Long, String> AGENT_ID_TO_PREFIX = new HashMap<>();

    /**
     * 有效前缀集合
     */
    private static final Set<String> VALID_PREFIXES = new HashSet<>();

    static {
        // SuperAgent: SA, ID=1
        PREFIX_TO_AGENT_TYPE.put("SA", AgentCommon.SuperAgent);
        PREFIX_TO_AGENT_ID.put("SA", AgentCommon.SuperAgentId);
        AGENT_TYPE_TO_PREFIX.put(AgentCommon.SuperAgent, "SA");
        AGENT_ID_TO_PREFIX.put(AgentCommon.SuperAgentId, "SA");

        // OpenManus: OM, ID=2
        PREFIX_TO_AGENT_TYPE.put("OM", AgentCommon.OpenManus);
        PREFIX_TO_AGENT_ID.put("OM", AgentCommon.OpenManusId);
        AGENT_TYPE_TO_PREFIX.put(AgentCommon.OpenManus, "OM");
        AGENT_ID_TO_PREFIX.put(AgentCommon.OpenManusId, "OM");

        // FamilyHarmony: FH, ID=3
        PREFIX_TO_AGENT_TYPE.put("FH", AgentCommon.FamilyHarmony);
        PREFIX_TO_AGENT_ID.put("FH", AgentCommon.FamilyHarmonyId);
        AGENT_TYPE_TO_PREFIX.put(AgentCommon.FamilyHarmony, "FH");
        AGENT_ID_TO_PREFIX.put(AgentCommon.FamilyHarmonyId, "FH");

        VALID_PREFIXES.addAll(Arrays.asList("SA", "OM", "FH"));
    }

    // ==================== 会话有效期常量 ====================

    /**
     * 默认Session有效期：30天（单位：秒）
     */
    public static final long SESSION_MAX_AGE_SECONDS = 30L * 24 * 60 * 60;

    // ==================== SessionId生成 ====================

    /**
     * 生成SuperAgent类型的SessionId
     *
     * @return 格式：SA + 8位时间戳 + 6位随机数 + 4位校验码（共20位）
     */
    public static String generateSuperAgentSessionId() {
        return generateSessionId(AGENT_TYPE_TO_PREFIX.get(AgentCommon.SuperAgent));
    }

    /**
     * 生成OpenManus类型的SessionId
     *
     * @return 格式：OM + 8位时间戳 + 6位随机数 + 4位校验码（共20位）
     */
    public static String generateOpenManusSessionId() {
        return generateSessionId(AGENT_TYPE_TO_PREFIX.get(AgentCommon.OpenManus));
    }

    /**
     * 生成FamilyHarmony类型的SessionId
     *
     * @return 格式：FH + 8位时间戳 + 6位随机数 + 4位校验码（共20位）
     */
    public static String generateFamilyHarmonySessionId() {
        return generateSessionId(AGENT_TYPE_TO_PREFIX.get(AgentCommon.FamilyHarmony));
    }

    /**
     * 根据Agent类型生成对应的SessionId
     *
     * @param agentType AgentCommon中定义的Agent类型常量
     * @return SessionId字符串
     * @throws IllegalArgumentException 如果Agent类型不支持
     */
    public static String generateSessionIdByType(String agentType) {
        String prefix = AGENT_TYPE_TO_PREFIX.get(agentType);
        if (prefix == null) {
            throw new IllegalArgumentException("不支持的Agent类型: " + agentType);
        }
        return generateSessionId(prefix);
    }

    /**
     * 根据AgentId生成对应的SessionId
     *
     * @param agentId AgentCommon中定义的Agent ID
     * @return SessionId字符串
     * @throws IllegalArgumentException 如果AgentId不支持
     */
    public static String generateSessionIdById(Long agentId) {
        String prefix = AGENT_ID_TO_PREFIX.get(agentId);
        if (prefix == null) {
            throw new IllegalArgumentException("不支持的AgentId: " + agentId);
        }
        return generateSessionId(prefix);
    }

    /**
     * SessionId核心生成方法
     *
     * <p>生成流程：</p>
     * <ol>
     *   <li>获取当前秒级时间戳，转换为36进制（8位）</li>
     *   <li>生成62进制随机数（6位）</li>
     *   <li>拼接前缀+时间戳+随机数作为载荷</li>
     *   <li>计算CRC32校验码（取前4位62进制字符）</li>
     *   <li>组合完整SessionId</li>
     * </ol>
     *
     * @param prefix Agent前缀（2字符）
     * @return 完整的SessionId（20字符）
     * @throws IllegalStateException 如果生成的SessionId长度超出限制
     */
    private static String generateSessionId(String prefix) {
        long seconds = Instant.now().getEpochSecond();
        String timestamp = encodeBase36(seconds, TIMESTAMP_LENGTH);
        String random = generateRandomBase62(RANDOM_LENGTH);
        String payload = prefix + timestamp + random;
        String checksum = calculateChecksum(payload);
        String sessionId = payload + checksum;

        if (sessionId.length() > MAX_LENGTH) {
            throw new IllegalStateException("生成的SessionId长度超出限制: " + sessionId.length());
        }

        return sessionId;
    }

    // ==================== 批量校验方法（新增）====================

    /**
     * 批量校验SessionId集合，区分有效与过期ID
     *
     * <p>适用于需要批量清理过期会话的场景，如定时任务、批量查询等</p>
     *
     * @param sessionIds SessionId集合（Set或List）
     * @return BatchValidationResult 包含有效ID集合和过期ID集合
     */
    public static BatchValidationResult validateBatch(Collection<String> sessionIds) {
        return validateBatch(sessionIds, null);
    }

    /**
     * 批量校验SessionId集合，包含AgentId一致性校验
     *
     * <p>适用于需要批量校验且验证Agent归属的场景</p>
     *
     * @param sessionIds SessionId集合（Set或List）
     * @param agentId AgentId（可为null，为null时不做Agent一致性校验）
     * @return BatchValidationResult 包含有效ID集合和过期/非法ID集合
     */
    public static BatchValidationResult validateBatch(Collection<String> sessionIds, Long agentId) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new BatchValidationResult(Collections.emptySet(), Collections.emptySet());
        }

        Set<String> validIds = new HashSet<>();
        Set<String> expiredIds = new HashSet<>();

        for (String sessionId : sessionIds) {
            ValidationResult result = validateSession(sessionId, agentId);
            if (result.isValid()) {
                validIds.add(sessionId);
            } else {
                expiredIds.add(sessionId);
            }
        }

        return new BatchValidationResult(validIds, expiredIds);
    }

    /**
     * 从SessionId集合中过滤出有效的ID（未过期且合法）
     *
     * <p>便捷方法，直接返回有效ID集合</p>
     *
     * @param sessionIds SessionId集合
     * @return 有效的SessionId集合
     */
    public static Set<String> filterValid(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return sessionIds.stream()
                .filter(SessionIdUtil::isValid)
                .collect(Collectors.toSet());
    }

    /**
     * 从SessionId集合中过滤出已过期的ID
     *
     * <p>便捷方法，直接返回过期ID集合</p>
     *
     * @param sessionIds SessionId集合
     * @return 已过期的SessionId集合
     */
    public static Set<String> filterExpired(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return sessionIds.stream()
                .filter(id -> !isValid(id))
                .collect(Collectors.toSet());
    }

    // ==================== SessionId校验（双重校验机制）====================

    /**
     * 基础校验：校验SessionId本身的合法性
     *
     * <p>校验内容：</p>
     * <ul>
     *   <li>格式与长度合规性</li>
     *   <li>前缀有效性（SA/OM/FH）</li>
     *   <li>字符集合规性（62进制）</li>
     *   <li>CRC32校验码正确性</li>
     *   <li>时间戳有效性（非未来时间）</li>
     *   <li>有效期检查（默认30天）</li>
     * </ul>
     *
     * @param sessionId 待校验的SessionId
     * @return ValidationResult 校验结果详情
     */
    public static ValidationResult validateSession(String sessionId) {
        return validateSession(sessionId, null);
    }

    /**
     * 双重校验：SessionId合法性 + AgentId一致性
     *
     * <p>校验流程：</p>
     * <ol>
     *   <li>基础格式校验（长度、前缀、字符集）</li>
     *   <li>CRC32校验码验证</li>
     *   <li>时间戳解析与有效性检查</li>
     *   <li>防未来时间攻击检测</li>
     *   <li>有效期检查（严格模式，过期即失效）</li>
     *   <li>AgentId一致性校验（如提供）</li>
     * </ol>
     *
     * <p>注意：本方法不包含自动续期逻辑，过期直接返回expired状态</p>
     *
     * @param sessionId SessionId（来自请求）
     * @param agentId AgentId（来自请求参数，可为null）
     * @return ValidationResult 校验结果
     */
    public static ValidationResult validateSession(String sessionId, Long agentId) {
        // 第一重校验：基础合法性
        if (!isBasicValid(sessionId)) {
            return ValidationResult.invalid("SessionId格式非法或包含非法字符");
        }

        String payload = sessionId.substring(0, TOTAL_LENGTH - CHECKSUM_LENGTH);
        String checksum = sessionId.substring(TOTAL_LENGTH - CHECKSUM_LENGTH);

        if (!calculateChecksum(payload).equals(checksum)) {
            log.warn("SessionId校验码错误，疑似篡改: {}", sessionId);
            return ValidationResult.invalid("SessionId校验码错误，可能已被篡改");
        }

        String timestampStr = sessionId.substring(PREFIX_LENGTH, PREFIX_LENGTH + TIMESTAMP_LENGTH);
        long sessionTime;
        try {
            sessionTime = decodeBase36(timestampStr);
        } catch (Exception e) {
            log.warn("SessionId时间戳解析失败");
            return ValidationResult.invalid("SessionId时间戳解析失败");
        }

        long now = Instant.now().getEpochSecond();

        if (sessionTime > now) {
            log.warn("SessionId时间戳为未来时间，疑似伪造");
            return ValidationResult.invalid("SessionId时间戳为未来时间，疑似伪造");
        }

        long age = now - sessionTime;
        if (age > SESSION_MAX_AGE_SECONDS) {
            log.warn("SessionId已过期，请重新获取");
            return ValidationResult.expired("SessionId已过期，请重新获取");
        }

        long remainingSeconds = SESSION_MAX_AGE_SECONDS - age;

        // 第二重校验：AgentId一致性
        if (agentId != null) {
            Long sessionAgentId = getAgentIdBySessionId(sessionId);
            if (sessionAgentId == null) {
                return ValidationResult.invalid("无法识别SessionId对应的Agent类型");
            }
            if (!Objects.equals(sessionAgentId, agentId)) {
                return ValidationResult.invalid("SessionId与AgentId不匹配，拒绝访问");
            }
        }

        return ValidationResult.valid(sessionId, remainingSeconds);
    }

    /**
     * 快速校验SessionId是否合法且未过期
     *
     * @param sessionId 待校验的SessionId
     * @return true-合法且未过期，false-非法或已过期
     */
    public static boolean isValid(String sessionId) {
        ValidationResult result = validateSession(sessionId);
        return result.isValid() && !result.isExpired();
    }

    /**
     * 校验SessionId是否属于指定Agent类型
     *
     * @param sessionId SessionId
     * @param agentType AgentCommon中定义的Agent类型常量
     * @return true-属于该Agent类型且合法，false-不属于或非法
     */
    public static boolean isValidForAgent(String sessionId, String agentType) {
        ValidationResult result = validateSession(sessionId);
        if (!result.isValid() || result.isExpired()) {
            return false;
        }
        String expectedPrefix = AGENT_TYPE_TO_PREFIX.get(agentType);
        return expectedPrefix != null && sessionId.startsWith(expectedPrefix);
    }

    /**
     * 校验SessionId是否为SuperAgent类型
     *
     * @param sessionId SessionId
     * @return true-是SuperAgent类型且合法，false-不是或非法
     */
    public static boolean isSuperAgentSessionId(String sessionId) {
        return isValidForAgent(sessionId, AgentCommon.SuperAgent);
    }

    /**
     * 校验SessionId是否为OpenManus类型
     *
     * @param sessionId SessionId
     * @return true-是OpenManus类型且合法，false-不是或非法
     */
    public static boolean isOpenManusSessionId(String sessionId) {
        return isValidForAgent(sessionId, AgentCommon.OpenManus);
    }

    /**
     * 校验SessionId是否为FamilyHarmony类型
     *
     * @param sessionId SessionId
     * @return true-是FamilyHarmony类型且合法，false-不是或非法
     */
    public static boolean isFamilyHarmonySessionId(String sessionId) {
        return isValidForAgent(sessionId, AgentCommon.FamilyHarmony);
    }

    /**
     * 基础格式校验（不包含CRC和过期时间检查）
     *
     * <p>用于快速过滤明显非法的请求</p>
     *
     * @param sessionId 待校验的SessionId
     * @return true-基础格式合法，false-非法
     */
    private static boolean isBasicValid(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        if (sessionId.length() != TOTAL_LENGTH) {
            return false;
        }

        String prefix = sessionId.substring(0, PREFIX_LENGTH);
        if (!VALID_PREFIXES.contains(prefix)) {
            return false;
        }

        for (char c : sessionId.toCharArray()) {
            if (BASE62.indexOf(c) < 0) {
                return false;
            }
        }

        return true;
    }

    // ==================== 信息提取方法 ====================

    /**
     * 获取SessionId对应的Agent类型
     *
     * <p>注意：本方法不做有效性校验，如需校验请先调用validateSession</p>
     *
     * @param sessionId SessionId
     * @return Agent类型常量，无法识别返回null
     */
    public static String getAgentType(String sessionId) {
        return getAgentTypeBySessionId(sessionId);
    }

    /**
     * 根据SessionId获取Agent类型
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @return Agent类型常量，无法识别返回null
     */
    public static String getAgentTypeBySessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < PREFIX_LENGTH) {
            return null;
        }
        String prefix = sessionId.substring(0, PREFIX_LENGTH);
        return PREFIX_TO_AGENT_TYPE.get(prefix);
    }

    /**
     * 根据SessionId获取AgentId
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @return AgentId，无法识别返回null
     */
    public static Long getAgentIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < PREFIX_LENGTH) {
            return null;
        }
        String prefix = sessionId.substring(0, PREFIX_LENGTH);
        return PREFIX_TO_AGENT_ID.get(prefix);
    }

    /**
     * 提取SessionId生成时间（秒级时间戳）
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @return 生成时间的秒级时间戳，解析失败返回-1
     */
    public static long getCreateTime(String sessionId) {
        if (sessionId == null || sessionId.length() < PREFIX_LENGTH + TIMESTAMP_LENGTH) {
            return -1;
        }
        try {
            String timestampStr = sessionId.substring(PREFIX_LENGTH, PREFIX_LENGTH + TIMESTAMP_LENGTH);
            return decodeBase36(timestampStr);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 计算SessionId剩余有效期（使用默认30天）
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @return 剩余秒数，已过期或解析失败返回-1
     */
    public static long getRemainingTime(String sessionId) {
        return getRemainingTime(sessionId, SESSION_MAX_AGE_SECONDS);
    }

    /**
     * 计算SessionId剩余有效期
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @param maxAgeSeconds 最大有效期（秒）
     * @return 剩余秒数，已过期或解析失败返回-1
     */
    public static long getRemainingTime(String sessionId, long maxAgeSeconds) {
        long createTime = getCreateTime(sessionId);
        if (createTime < 0) {
            return -1;
        }
        long now = Instant.now().getEpochSecond();
        long age = now - createTime;
        if (age > maxAgeSeconds) {
            return -1;
        }
        return maxAgeSeconds - age;
    }

    // ==================== 扩展查询方法 ====================

    /**
     * 根据AgentId获取Agent类型
     *
     * @param agentId AgentId
     * @return Agent类型常量，未知返回null
     */
    public static String getAgentTypeByAgentId(Long agentId) {
        if (agentId == null) {
            return null;
        }
        String prefix = AGENT_ID_TO_PREFIX.get(agentId);
        return prefix != null ? PREFIX_TO_AGENT_TYPE.get(prefix) : null;
    }

    /**
     * 根据SessionId获取RedisKey前缀
     *
     * <p>注意：本方法不做有效性校验</p>
     *
     * @param sessionId SessionId
     * @return RedisKey前缀，无法识别返回null
     */
    public static String getRedisKeyPrefixBySessionId(String sessionId) {
        String agentType = getAgentTypeBySessionId(sessionId);
        if (agentType == null) {
            log.error("无法识别SessionId对应的Agent类型: {}", sessionId);
            return null;
        }

        if (agentType.equals(AgentCommon.SuperAgent)) {
            return AgentCommon.RedisSuperAgentKeyPrefix;
        } else if (agentType.equals(AgentCommon.OpenManus)) {
            return AgentCommon.RedisManusKeyPrefix;
        } else if (agentType.equals(AgentCommon.FamilyHarmony)) {
            return AgentCommon.RedisFamilyHarmonyKeyPrefix;
        } else {
            log.error("未知的Agent类型: {}", agentType);
            return null;
        }
    }

    public static String getRedisKeyPrefixByAgentId(Long agentId) {
        if (AgentCommon.SuperAgentId.equals(agentId)) {
            return AgentCommon.RedisSuperAgentKeyPrefix;
        }else if (AgentCommon.OpenManusId.equals(agentId)) {
            return AgentCommon.RedisManusKeyPrefix;
        }else if (AgentCommon.FamilyHarmonyId.equals(agentId)) {
            return AgentCommon.RedisFamilyHarmonyKeyPrefix;
        }
        return null;
    }

    // ==================== 结果封装类 ====================

    /**
     * Session校验结果
     *
     * <p>封装单次校验的完整结果信息</p>
     */
    public static class ValidationResult {

        private final boolean valid;
        private final boolean expired;
        private final String message;
        private final String sessionId;
        private final long remainingSeconds;

        private ValidationResult(boolean valid, boolean expired, String message,
                                 String sessionId, long remainingSeconds) {
            this.valid = valid;
            this.expired = expired;
            this.message = message;
            this.sessionId = sessionId;
            this.remainingSeconds = remainingSeconds;
        }

        public static ValidationResult valid(String sessionId, long remainingSeconds) {
            return new ValidationResult(true, false, "校验通过", sessionId, remainingSeconds);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, false, message, null, 0);
        }

        public static ValidationResult expired(String message) {
            return new ValidationResult(false, true, message, null, 0);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isExpired() {
            return expired;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getRemainingSeconds() {
            return remainingSeconds;
        }

        /**
         * 判断是否需要重新登录
         *
         * @return true-需要重新登录（已过期或非法）
         */
        public boolean needReLogin() {
            return !valid || expired;
        }
    }

    /**
     * 批量校验结果
     *
     * <p>封装批量校验后的有效ID集合与过期ID集合</p>
     */
    public static class BatchValidationResult {

        private final Set<String> validIds;
        private final Set<String> expiredIds;

        public BatchValidationResult(Set<String> validIds, Set<String> expiredIds) {
            this.validIds = validIds != null ? validIds : Collections.emptySet();
            this.expiredIds = expiredIds != null ? expiredIds : Collections.emptySet();
        }

        /**
         * 获取有效的SessionId集合
         *
         * @return 未过期且合法的SessionId集合
         */
        public Set<String> getValidIds() {
            return validIds;
        }

        /**
         * 获取过期或非法的SessionId集合
         *
         * @return 已过期或校验失败的SessionId集合
         */
        public Set<String> getExpiredIds() {
            return expiredIds;
        }

        /**
         * 获取有效ID数量
         *
         * @return 有效SessionId数量
         */
        public int getValidCount() {
            return validIds.size();
        }

        /**
         * 获取过期ID数量
         *
         * @return 过期SessionId数量
         */
        public int getExpiredCount() {
            return expiredIds.size();
        }

        /**
         * 判断是否全部有效
         *
         * @return true-全部有效，false-存在过期ID
         */
        public boolean isAllValid() {
            return expiredIds.isEmpty();
        }

        /**
         * 判断是否全部过期
         *
         * @return true-全部过期，false-存在有效ID
         */
        public boolean isAllExpired() {
            return validIds.isEmpty();
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 生成指定长度的62进制随机字符串
     *
     * @param length 目标长度
     * @return 随机字符串
     */
    private static String generateRandomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62.charAt(SECURE_RANDOM.nextInt(BASE62_LENGTH)));
        }
        return sb.toString();
    }

    /**
     * 将long值编码为指定长度的36进制字符串（左补零）
     *
     * @param value 待编码的数值
     * @param length 目标长度
     * @return 36进制字符串
     */
    private static String encodeBase36(long value, int length) {
        String encoded = Long.toString(value, 36).toUpperCase();
        if (encoded.length() > length) {
            //TODO:需要手动处理
            log.warn("时间戳超出36进制{}位限制", length);
            encoded = encoded.substring(encoded.length() - length);
        }
        return String.format("%" + length + "s", encoded).replace(' ', '0').toUpperCase();
    }

    /**
     * 将36进制字符串解码为long值
     *
     * @param str 36进制字符串
     * @return 解码后的long值
     */
    private static long decodeBase36(String str) {
        return Long.parseLong(str.toLowerCase(), 36);
    }

    /**
     * 计算CRC32校验码，取前4位62进制字符
     *
     * @param payload 待计算的数据载荷
     * @return 4位62进制校验码
     */
    private static String calculateChecksum(String payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long value = crc32.getValue();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CHECKSUM_LENGTH; i++) {
            sb.append(BASE62.charAt((int) (value % BASE62_LENGTH)));
            value /= BASE62_LENGTH;
        }
        return sb.toString();
    }
}