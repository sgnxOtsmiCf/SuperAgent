package cn.sgnxotsmicf.app.superagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class InterceptorRegistry {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ZhiPuAiChatModel zhiPuAiChatModel;

    @Resource
    private ModelLoggingInterceptor modelLoggingInterceptor;

    @Resource
    private ToolMonitoringInterceptor toolMonitoringInterceptor;

    @Resource
    private ToolCacheInterceptor toolCacheInterceptor;

    public List<Interceptor> buildInterceptors() {
        return Arrays.asList(
                TodoListInterceptor.builder().build(),
                ToolSelectionInterceptor.builder().selectionModel(zhiPuAiChatModel).build(),
                buildToolRetryInterceptor(),
                modelLoggingInterceptor,
                toolMonitoringInterceptor,
                toolCacheInterceptor
        );
    }

    private ToolRetryInterceptor buildToolRetryInterceptor() {
        return ToolRetryInterceptor.builder()
                .maxRetries(2)
                .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                .build();
    }
}