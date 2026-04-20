package cn.sgnxotsmicf.common.version;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:32
 * @Version: 1.0
 * @Description: SuperAgent优点和特性
 */


@Component
@Slf4j
public class SuperAgentAdvantage {

    @Resource
    private SuperAgentVersion superAgentVersion;

    private final Map<String, String> advantage = Map.of(
            "多Agent架构支持","支持SuperAgent、Manus、Family等多个智能体，可根据不同场景灵活切换",
            "流式响应体验","采用SSE流式传输，实时展示AI思考过程、工具调用和响应内容，用户体验流畅",
            "完整的记忆系统","基于Redis的分布式会话存储，支持消息持久化、用户画像、历史会话回溯和归档管理",
            "工具链生态丰富","内置多种工具，支持工具自动调用和结果展示",
            "动态提示词拦截","支持通过拦截器动态修改系统提示词，实现个性化Agent行为定制",
            "Hook机制扩展","提供完整的Hook注册机制，支持在Agent生命周期各阶段插入自定义逻辑",
            "前端交互友好","现代化的Vue3界面，支持Markdown渲染、代码高亮、消息操作等丰富交互",
            "功能完备","支持用户认证、会话置顶、消息分享、文件上传等企业级功能"
    );

    public Map<String, String> getAdvantage() {
        return advantage;
    }
}
