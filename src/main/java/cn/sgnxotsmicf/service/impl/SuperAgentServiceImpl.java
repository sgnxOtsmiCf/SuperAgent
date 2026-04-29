package cn.sgnxotsmicf.service.impl;


import cn.sgnxotsmicf.app.superagent.SuperAgent;
import cn.sgnxotsmicf.common.auth.UserInfoContext;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.SuperAgentService;
import cn.sgnxotsmicf.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 17:01
 * @Version: 1.0
 * @Description:
 */
@Service
@RequiredArgsConstructor
public class SuperAgentServiceImpl implements SuperAgentService {

    private final SuperAgent superAgent;

    private final UserService userService;

    private final ServiceUtil serviceUtil;

    private final ChatSessionService chatSessionService;


    @Override
    public SseEmitter doChatStream(ChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        Long agentId = request.getAgentId();
        //创建 SSE 实例，设置超时时间（30分钟，避免长连接断开）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        //获取用户id
        Long userId = serviceUtil.getUserId();
        //初始化session状态:判断非空、绑定UserId、更新session会话时间
        List<Object> objectList = serviceUtil.initSessionIdToUserIdOrUpdate(sessionId, userId, message, agentId, chatSessionService, emitter);
        emitter = (SseEmitter) objectList.getFirst();
        sessionId = objectList.getLast().toString();
        //判断sessionId是否是该用户的
        boolean flag = serviceUtil.judgeSessionIdToUserIdIsExist(sessionId, userId);
        if (!flag) {
            throw new AgentException(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        //调用模型
        request.setSessionId(sessionId);
        superAgent.doChatAgent(request, emitter, userId, UserInfoContext.getCurrentUsername());
        return emitter;
    }

    private String getUserNameByUserId(Long userId) {
        return userService
                .getOne(new LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .select(User::getUsername))
                .getUsername();
    }

}
