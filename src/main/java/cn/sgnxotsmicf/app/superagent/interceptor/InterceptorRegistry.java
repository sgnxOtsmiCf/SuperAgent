package cn.sgnxotsmicf.app.superagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InterceptorRegistry {

    private final ZhiPuAiChatModel zhiPuAiChatModel;

    private final ModelLoggingInterceptor modelLoggingInterceptor;

    private final ToolMonitoringInterceptor toolMonitoringInterceptor;

    private final ToolCacheInterceptor toolCacheInterceptor;

    private final ReActTokenUsageInterceptor reActTokenUsageInterceptor;

    public List<Interceptor> buildInterceptors() {
        return Arrays.asList(
                modelLoggingInterceptor,
                //reActTokenUsageInterceptor,
                TodoListInterceptor.builder().build(),
                ToolSelectionInterceptor.builder().selectionModel(zhiPuAiChatModel).build(),
                buildToolRetryInterceptor(),
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