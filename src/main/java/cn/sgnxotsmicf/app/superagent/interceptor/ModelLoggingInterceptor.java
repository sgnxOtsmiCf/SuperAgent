package cn.sgnxotsmicf.app.superagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModelLoggingInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        log.warn("发送请求到模型: {} 条消息", request.getMessages().size());

        long startTime = System.currentTimeMillis();

        // 执行实际调用
        ModelResponse response = handler.call(request);

        // 响应后记录
        long duration = System.currentTimeMillis() - startTime;
        log.warn("模型响应耗时: {}ms", duration);

        return response;
    }

    @Override
    public String getName() {
        return "LoggingInterceptor";
    }
}