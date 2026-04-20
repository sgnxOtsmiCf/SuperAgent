package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.advisor.AdvisorRegister;
import cn.sgnxotsmicf.app.family.FamilyHarmony;
import cn.sgnxotsmicf.app.manus.Manus;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.AgentCommon;
import cn.sgnxotsmicf.app.ChatClientFactory;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.OpenManusService;
import cn.sgnxotsmicf.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


/**
 * @Author: lixiang
 * @CreateDate: 2026/4/8 21:05
 * @Version: 1.0
 * @Description:
 */
@Service
public class OpenManusServiceImpl implements OpenManusService {

    private final ChatSessionService chatSessionService;

    private final UserService userService;

    private final ChatClientFactory chatClientFactory;

    private final FamilyHarmony familyHarmony;

    private final Manus manus;

    private final ServiceUtil serviceUtil;

    private final DeepSeekChatModel deepSeekChatModel;

    private final AdvisorRegister advisorRegister;

    public OpenManusServiceImpl(ChatSessionService chatSessionService, FamilyHarmony familyHarmony, Manus manus, ChatClientFactory chatClientFactory, UserService userService, ServiceUtil serviceUtil, DeepSeekChatModel deepSeekChatModel, AdvisorRegister advisorRegister) {
        this.chatSessionService = chatSessionService;
        this.userService = userService;
        this.chatClientFactory = chatClientFactory;
        this.serviceUtil = serviceUtil;
        this.familyHarmony = familyHarmony;
        this.manus = manus;
        this.deepSeekChatModel = deepSeekChatModel;
        this.advisorRegister = advisorRegister;
    }

    @Override
    public boolean sessionIdIsExist(String sessionId) {
        return 1 == chatSessionService.getBaseMapper().selectCount(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId));
    }

    @Override
    public SseEmitter doChatWithSseEmitter(ChatRequest chatRequest) {
        String message = chatRequest.getMessage();
        String sessionId = chatRequest.getSessionId();
        Long agentId = chatRequest.getAgentId();
        //获取用户id
        Long userId = serviceUtil.getUserId();
        //创建 SSE 实例，设置超时时间（30分钟，避免长连接断开）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        //初始化session状态:判断非空、绑定UserId
        List<Object> objectList = serviceUtil.initSessionIdToUserIdOrUpdate(sessionId, userId, message, agentId, chatSessionService, emitter);
        emitter = (SseEmitter) objectList.getFirst();
        sessionId = objectList.getLast().toString();
        //判断sessionId存在
        boolean flag = serviceUtil.judgeSessionIdToUserIdIsExist(sessionId, userId);
        if (!flag) {
            throw new AgentException(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        ChatMemory redisChatMemory = serviceUtil.getRedisChatMemory(userId, agentId);
        // 初始化客户端
        if (AgentCommon.OpenManusId.equals(agentId)) {
            ChatClient chatClient = chatClientFactory.getChatClient(userId, getUserNameByUserId(userId), redisChatMemory, advisorRegister.buildAdvisors(manus.getMaxSteps()));
            manus.setChatClient(chatClient);
            return manus.runStream(message, sessionId, emitter);
        }
        ChatClient chatClient = chatClientFactory
                .getChatClient(userId, getUserNameByUserId(userId), redisChatMemory, advisorRegister.buildAdvisors(familyHarmony.getMaxSteps()), true);
        familyHarmony.setChatClient(chatClient);
        return familyHarmony.runStream(message, sessionId, emitter);
    }

    private String getUserNameByUserId(Long userId) {
        return userService
                .getOne(new LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .select(User::getUsername))
                .getUsername();
    }
}
