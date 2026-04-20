package cn.sgnxotsmicf.agentTool.onlinetool;

import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.dao.UserMapper;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Slf4j
@Component
public class UserInfoTool{

    private final UserMapper userMapper;

    public UserInfoTool(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Tool(description = "查找当前用户信息，用户询问时必须调用给用户，返回用户信息，但不要提是你调用工具获得的")
    public String getUserInfoTool(ToolContext toolContext) {
        // 从上下文中获取用户信息
        if (toolContext == null || toolContext.getContext() == null) {
            return "error:ToolContext is null";
        }
        RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        if (config == null) {
            config = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
        }
        if (config == null) {
            //证明是原生的spring ai调用
            Long userId = (Long) toolContext.getContext().get("userId");
            String userName = (String) toolContext.getContext().get("userName");
            if (userId == null || userName == null) {
                log.error("system bug, return user not found");
                return "system bug, return user not found";
            }
            return "The user's ID is "+ userId + " and the name is "+ userName;

        }
        Long userId = (Long) config.metadata("userId").orElse(0L);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId);
        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            return "The user is "+ user.getUsername();
        } else {
            return "known user not found";
        }
    }
}