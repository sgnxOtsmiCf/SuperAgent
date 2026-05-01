package cn.sgnxotsmicf.common.tools;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.chatMemory.NoSqlChatMemoryFactory;
import cn.sgnxotsmicf.common.po.ChatMessage;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.rabbitmq.constant.MqConst;
import cn.sgnxotsmicf.common.rabbitmq.entity.SessionMessage;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.ChatSessionService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.config.Config;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/10 14:42
 * @Version: 1.0
 * @Description: service层的核心功能工具
 */

@Slf4j
@Component
public class ServiceUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private NoSqlChatMemoryFactory noSqlChatMemoryFactory;

    @Resource
    private Config config;

    @Resource
    private RabbitService rabbitService;


    /**
     * 获取用户id,之后会重构
     *
     * @return userId
     */
    public Long getUserId() {
        long userId;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.GET_USERID_FAIL);
        }
        return userId;
    }


    /**
     * 进行会话的初始化绑定，存入到MySql这一步之后使用消息队列进行通知
     *
     * @param sessionId          会话id
     * @param userId             用户id
     * @param message            用户消息
     * @param agentId            智能体id
     * @param chatSessionService 会话服务层
     */
    public void bindSessionIdToUserId(String sessionId, Long userId, String message, Long agentId, ChatSessionService chatSessionService) {
        //存入到redis中
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);

        stringRedisTemplate.opsForZSet().add(redisKeyPrefix + userId.toString(), sessionId, System.currentTimeMillis());
        String sessionName = message.length() > 10 ? message.substring(0, 10) : message;
        stringRedisTemplate.opsForHash().putAll(redisKeyPrefix + sessionId,
                Map.of("sessionName", sessionName,
                        //"lastActive", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "agentId", String.valueOf(agentId),
                        "session_status", "1",
                        "isTop", ""));

        SessionMessage sessionMessage = SessionMessage.createInsertMessage(sessionId, userId, agentId, sessionName);

        sendChatSession(sessionMessage);
    }


    /**
     * 使用rabbitmq异步的消费ChatSession，存入数据库中
     * @param sessionMessage  SessionMessage对象
     */
    public void sendChatSession(SessionMessage sessionMessage){
        rabbitService.sendMessage(MqConst.EXCHANGE_CHAT_SESSION_DB, MqConst.ROUTING_CHAT_SESSION_DB,
                sessionMessage);
    }


    /**
     * 更新会话最后活跃时间
     *
     * @param sessionId          会话id
     * @param chatSessionService 会话服务层
     */
    public void updateSessionLastActive(Long userId, String sessionId, ChatSessionService chatSessionService) {
        //更新到redis中
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        assert redisKeyPrefix != null;
        stringRedisTemplate.opsForZSet().add(redisKeyPrefix + userId.toString(), sessionId, System.currentTimeMillis());
        SessionMessage sessionMessage = SessionMessage.createStatusUpdateMessage(
                sessionId, userId, -1L, null, SessionMessage.STATUS_ACTIVE);
        sendChatSession(sessionMessage);
    }


    /**
     * 从redis中进行判断，判断是否用户存在此会话id
     *
     * @param sessionId 会话
     * @param userId    用户id
     * @return 是否存在
     */
    public boolean judgeSessionIdToUserIdIsExist(String sessionId, Long userId) {
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        Double score = stringRedisTemplate.opsForZSet().score(redisKeyPrefix + userId.toString(), sessionId);
        return score != null;
    }


    /**
     * 核心会话初始化---每一次消息都要经过这里
     *
     * @param sessionId          会话id
     * @param userId             用户id
     * @param message            用户消息
     * @param agentId            智能体id
     * @param chatSessionService 会话服务层
     * @param emitter            流式传输
     * @return emitter, sessionId(为什么要返回，可能涉及到)数组
     */
    public List<Object> initSessionIdToUserIdOrUpdate(String sessionId, Long userId, String message, Long agentId, ChatSessionService chatSessionService, SseEmitter emitter) {
        if (StrUtil.isEmpty(sessionId)) {
            sessionId = SessionIdUtil.generateSessionIdByType(SessionIdUtil.getAgentTypeByAgentId(agentId));
            bindSessionIdToUserId(sessionId, userId, message, agentId, chatSessionService);
            try {
                Map<String, String> map = new HashMap<>();
                map.put("sessionId", sessionId);
                String jsonStr = JSONUtil.toJsonStr(map);
                emitter.send(SseEmitter.event()
                        .name("init")
                        .data(jsonStr)
                        .build());
                return List.of(emitter, sessionId);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return List.of(emitter, sessionId);
        }
        //校准sessionId
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId);
        try {
            if (validationResult.isExpired()) {

                emitter.send(SseEmitter.event()
                        .name("sessionIdException")
                        .data(Result.build(ResultCodeEnum.SESSION_ID_EXPIRED))
                        .build());
                emitter.complete();

            } else if (!validationResult.isValid()) {
                emitter.send(SseEmitter.event()
                        .name("sessionIdException")
                        .data(Result.build(ResultCodeEnum.SESSION_NO_FIND))
                        .build());
                emitter.complete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateSessionLastActive(userId, sessionId, chatSessionService);
        return List.of(emitter, sessionId);
    }


    /**
     * 更新会话标题
     * @param sessionId 会话id
     * @param sessionName 会话标题
     * @param chatSessionService 会话服务层
     * @param isOnlyRedis 是否只更新redis
     */
    public void updateSessionSessionName(String sessionId, String sessionName, ChatSessionService chatSessionService, boolean isOnlyRedis) {
        //同步更新redis和mysql---之后优化为同步更新redis，异步更新mysql| 或者先更新mysql，在删除redis
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        stringRedisTemplate.opsForHash().put(redisKeyPrefix + sessionId, "sessionName", sessionName);
        if (!isOnlyRedis) {
            chatSessionService
                    .update(new LambdaUpdateWrapper<ChatSession>()
                            .set(ChatSession::getSessionName, sessionName)
                            .eq(ChatSession::getSessionId, sessionId));
        }
    }


    /**
     * 更新归档会话标题
     * @param sessionId 会话id
     * @param sessionName 会话标题
     * @param chatSessionService 会话服务层
     */
    public void updateSessionSessionNameArchive(String sessionId, String sessionName, ChatSessionService chatSessionService) {
        chatSessionService
                .update(new LambdaUpdateWrapper<ChatSession>()
                        .set(ChatSession::getSessionName, sessionName)
                        .eq(ChatSession::getSessionId, sessionId)
                        .eq(ChatSession::getSessionStatus, 0)
                        .eq(ChatSession::getIsDeleted, 0));
    }


    /**
     * 设置会话置顶
     * @param sessionId 会话id
     * @param isTop 是否置顶
     */
    public void setIsTopSession(String sessionId, boolean isTop) {
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        if (isTop) {
            //置顶
            stringRedisTemplate.opsForHash().put(redisKeyPrefix + sessionId, "isTop", String.valueOf(System.currentTimeMillis()));
        } else {
            stringRedisTemplate.opsForHash().put(redisKeyPrefix + sessionId, "isTop", "");
        }
    }


    /**
     * spring ai alibaba 的redis获取数据并转换为List<ChatSessionVo>的方法
     * @param sessionIdList      会话列表
     * @param redisSaver         存储对象
     * @param chatSessionService 服务层对象
     * @return List<ChatSessionVo>对象
     */
    public List<ChatSessionVo> getChatSessionVoList(Set<String> sessionIdList, RedisSaver redisSaver, ChatSessionService chatSessionService) {
        if (sessionIdList == null || sessionIdList.isEmpty()) {
            return Collections.emptyList();
        }
        SessionIdUtil.BatchValidationResult batchValidationResult = SessionIdUtil.validateBatch(sessionIdList);
        sessionIdList = batchValidationResult.getValidIds();
        if (batchValidationResult.getExpiredCount() > 0) {
            //异步处理
            log.info("{} expired sessions", batchValidationResult.getExpiredCount());
        }
        List<ChatSessionVo> sessionVoList = new ArrayList<>();
        for (String sessionId : sessionIdList) {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(sessionId).build();
            Optional<Checkpoint> optionalCheckpoint = redisSaver.get(runnableConfig);
            optionalCheckpoint.ifPresent(checkpoint -> {
                ArrayList<Message> list = (ArrayList) checkpoint.getState().get("messages");
                List<ChatMessageVo> chatMessageVoList = list.stream().map(this::getChatMessageVoByMessage).collect(Collectors.toList());
                ChatSessionVo chatSessionVo = getChatSessionVo(sessionId, chatMessageVoList, chatSessionService);
                if (chatSessionVo != null) {
                    sessionVoList.add(chatSessionVo);
                }
                //id state nodeId nextNodeId -> state.messages: {media-[] messageType textContent metadata}
            });
        }
        //1. 置顶时间最新的
        //2. 置顶时间较早的
        //3. 未置顶但最近活跃的
        //4. 未置顶且不活跃的/null
        sessionVoList.sort(
                Comparator
                        // 第一步：按是否置顶分组（true=未置顶排后面，false=有置顶排前面）
                        .comparing((ChatSessionVo s) -> {
                            String top = s.getIsTop();
                            return top == null || top.isEmpty();
                        })
                        // 第二步：置顶组内按置顶时间倒序，非置顶组内按最后活跃时间倒序
                        .thenComparing((ChatSessionVo s) -> {
                            String top = s.getIsTop();
                            if (top != null && !top.isEmpty()) {
                                // 置顶项：返回置顶时间（Long类型），后面用reverseOrder倒序
                                return Long.parseLong(top);
                            } else {
                                // 非置顶项：返回最后活跃时间的时间戳，null给默认值0（会被nullsLast处理）
                                LocalDateTime active = s.getLastActive();
                                return active != null ? active.toEpochSecond(ZoneOffset.UTC) : Long.MIN_VALUE;
                            }
                        }, Comparator.reverseOrder())
        );
        //过滤
        return sessionVoList.stream().filter(chatSessionVo -> {
            if (chatSessionVo.getSessionId() == null) {
                return false;
            }
            if (StrUtil.isEmpty(chatSessionVo.getSessionName())) {
                return false;
            }
            if (chatSessionVo.getContent() == null || chatSessionVo.getContent().isEmpty()) {
                return false;
            }
            return chatSessionVo.getAgentId() != null;
        }).collect(Collectors.toList());
    }



    /**
     * 根据List<ChatMessageVo>对象直接构造ChatSessionVo---活跃状态下才可以，归档不要使用
     * @param sessionId 会话id
     * @param chatMessageVoList List<ChatMessageVo>对象
     * @param chatSessionService 会话服务层
     * @return ChatSessionVo对象
     */
    public ChatSessionVo getChatSessionVo(String sessionId, List<ChatMessageVo> chatMessageVoList, ChatSessionService chatSessionService) {
        String RedisKey = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        Map<String, String> sessionInfo = getSessionFields(sessionId, RedisKey, "sessionName", "agentId", "session_status", "isTop");
        String session_status_string = sessionInfo.get("session_status");
        if (session_status_string != null) {
            int session_status = Integer.parseInt(session_status_string);
            if (session_status != 1) {
                //不是活跃状态
                return null;
            }
        }

        ChatSessionVo chatSessionVo = new ChatSessionVo();
        chatSessionVo.setSessionId(sessionId); //添加id
        String sessionName = sessionInfo.get("sessionName");
        if (StrUtil.isEmpty(sessionName)) {
            //降级查询sessionName
            ChatSession chatSession = chatSessionService
                    .getOne(new LambdaQueryWrapper<ChatSession>().select(ChatSession::getSessionName).eq(ChatSession::getSessionId, sessionId));
            if (chatSession == null) {
                //如果到这里，明显出现了严重的不一致现象,需要排查
                throw new AgentException(ResultCodeEnum.AGENT_FAIL);
            }
            sessionName = chatSession.getSessionName();
            updateSessionSessionName(sessionId, sessionName, chatSessionService, true);
            chatSessionVo.setSessionName(sessionName);//添加sessionName
        } else {
            chatSessionVo.setSessionName(sessionName);
        }
        Long userId = getUserId();
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        Double score = stringRedisTemplate.opsForZSet().score(redisKeyPrefix + userId, sessionId);
        if (score != null) {
            chatSessionVo.setLastActive(TimeConverter.toLocalDateTime(score));
        }
        String agentIdStr = sessionInfo.get("agentId");//添加agentId
        long agentId = (agentIdStr != null && !agentIdStr.isBlank()) ? Long.parseLong(agentIdStr) : 0L;
        if (agentId != 0) {
            chatSessionVo.setAgentId(agentId);
        }
        if (chatMessageVoList != null && !chatMessageVoList.isEmpty()) {
            chatSessionVo.setContent(chatMessageVoList);//添加chatMessageVoList
        }
        String isTop = sessionInfo.get("isTop"); //添加置顶功能
        chatSessionVo.setIsTop(isTop);
        return chatSessionVo;
    }


    /**
     * 从redis中批量获取Hash中的指定字段，用于会话的补充查询
     * @param sessionId 会话id
     * @param redisKeyPrefix redisKey前缀
     * @param fields 小key属性
     * @return 返回从redis查到的map对象
     */
    public Map<String, String> getSessionFields(String sessionId, String redisKeyPrefix, String... fields) {
        List<Object> fieldList = Arrays.asList(fields);
        List<Object> values = stringRedisTemplate.opsForHash()
                .multiGet(redisKeyPrefix + sessionId, fieldList);
        if (values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            result.put(fields[i], values.get(i) != null ? values.get(i).toString() : null);
        }
        return result;
    }


    /**
     * 根据Message对象来获取ChatMessageVo
     * @param message Message对象
     * @return ChatMessageVo 对象
     */
    public ChatMessageVo getChatMessageVoByMessage(Message message) {
        ChatMessageVo chatMessageVo = new ChatMessageVo();
        //TODO:这里两个值spring ai alibaba redis中都没有存,用默认值代替|因为也不影响前端展示效果
        chatMessageVo.setMessageTime(LocalDateTime.now());
        chatMessageVo.setId(-1L);

        switch (message.getMessageType()) {
            case USER -> {
                UserMessage userMessage = (UserMessage) message;
                chatMessageVo.setContent(userMessage.getText());
                chatMessageVo.setMessageType(userMessage.getMessageType().name());
                chatMessageVo.setMetadata(userMessage.getMetadata());
            }
            case ASSISTANT -> {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                chatMessageVo.setContent(assistantMessage.getText());
                chatMessageVo.setMessageType(assistantMessage.getMessageType().name());

                Map<String, Object> metadata = assistantMessage.getMetadata();
                List<Media> media = assistantMessage.getMedia();
                metadata.put("toolCalls", assistantMessage.getToolCalls());
                metadata.put("media", media);
                chatMessageVo.setMetadata(metadata);

                // 二级校验：特定厂商AssistantMessage扩展处理
                enrichAssistantMetadata(metadata, assistantMessage);
            }
            case TOOL -> {
                ToolResponseMessage toolResponseMessage = (ToolResponseMessage) message;
                List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
                chatMessageVo.setMessageType(toolResponseMessage.getMessageType().name());
                chatMessageVo.setMetadata(toolResponseMessage.getMetadata());
                chatMessageVo.setContent(responses);
            }
        }
        return chatMessageVo;
    }

    /**
     * 二级校验：针对特定厂商的AssistantMessage进行元数据扩展
     * @param metadata Map对象的元信息
     * @param assistantMessage AssistantMessage对象
     */
    private void enrichAssistantMetadata(Map<String, Object> metadata, AssistantMessage assistantMessage) {
        Object existingReasoning = metadata.get("reasoningContent");
        if (existingReasoning != null && !existingReasoning.toString().isEmpty()) {
            return;
        }
        if (assistantMessage instanceof ZhiPuAiAssistantMessage zhiPuAiAssistantMessage) {
            String reasoningContent = zhiPuAiAssistantMessage.getReasoningContent();
            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                metadata.put("reasoningContent", reasoningContent);
            }
        } else if (assistantMessage instanceof DeepSeekAssistantMessage deepSeekAssistantMessage) {
            String reasoningContent = deepSeekAssistantMessage.getReasoningContent();
            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                metadata.put("reasoningContent", reasoningContent);
            }
        } else {
            try {
                java.lang.reflect.Method method = assistantMessage.getClass().getMethod("getReasoningContent");
                Object reasoningContent = method.invoke(assistantMessage);
                if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                    metadata.put("reasoningContent", reasoningContent.toString());
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.warn("反射提取reasoningContent失败: {}", e.getMessage());
            }
        }
    }


    /**
     *  根据List<Message>对象获取到List<ChatMessage>对象
     * @param messageList List<Message>对象
     * @param conversationId 会话id
     * @return List<ChatMessage>
     */
    public List<ChatMessage> convertToChatMessage(List<Message> messageList, String conversationId) {
        return messageList.stream().map(po -> {
            MessageType type = po.getMessageType();
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(conversationId);
            chatMessage.setMessageType(po.getMessageType().name());
            chatMessage.setContent(po.getText());
            chatMessage.setMessageTime(LocalDateTime.now());
            //TODO:这里setId就不需要指定了，直接使用mysql的自增模式，简单高效，防止页分裂,下面方法同理
            switch (type) {
                case USER -> chatMessage.setMetadata(po.getMetadata());
                case ASSISTANT -> {
                    AssistantMessage assistantMessage = (AssistantMessage) po;
                    List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
                    Map<String, Object> metadata = assistantMessage.getMetadata();
                    List<Media> media = assistantMessage.getMedia();
                    metadata.put("toolCalls", toolCalls);
                    metadata.put("media", media);
                    chatMessage.setMetadata(metadata);
                }
                case TOOL -> {
                    ToolResponseMessage toolResponseMessage = (ToolResponseMessage) po;
                    List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
                    String responsesJsonStr = JSONUtil.toJsonStr(responses);
                    chatMessage.setMessageType(toolResponseMessage.getMessageType().name());
                    chatMessage.setMetadata(toolResponseMessage.getMetadata());
                    //TODO:这里这个解析有问题,将道理应该Context类型应该是Object,但现在有点债太多了，不敢改了。
                    chatMessage.setContent(responsesJsonStr);
                }
            }
            return chatMessage;
        }).collect(Collectors.toList());
    }


    /**
     * 获取spring ai alibaba的RedisChatMemory的ChatMemory对象
     * @param userId 用户di
     * @param agentId 智能体id
     * @return ChatMemory对象
     */
    public ChatMemory getRedisChatMemory(Long userId, Long agentId) {
        return noSqlChatMemoryFactory.getRedissonChatMemory(noSqlChatMemoryFactory.getRedissonRedisChatMemoryRepository(config, userId, agentId));
    }


    /**
     * 获取spring ai alibaba的RedisChatMemoryRepository的ChatMemoryRepository对象
     * @param userId 用户id
     * @param agentId 智能体id
     * @return ChatMemoryRepository对象
     */
    public ChatMemoryRepository getRedissonRedisChatMemoryRepository(Long userId, Long agentId) {
        return noSqlChatMemoryFactory.getRedissonRedisChatMemoryRepository(config, userId, agentId);
    }
}
