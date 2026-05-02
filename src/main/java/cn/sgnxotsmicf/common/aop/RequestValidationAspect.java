package cn.sgnxotsmicf.common.aop;

import cn.hutool.core.util.StrUtil;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import cn.sgnxotsmicf.exception.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 20:16
 * @Version: 1.0
 * @Description: 校验Ai对话的请求参数
 */

@Slf4j
@Aspect
@Component
public class RequestValidationAspect {

    @Before("@annotation(requestValidation)")
    public void before(JoinPoint joinPoint, RequestValidation requestValidation) {
        String type = requestValidation.type();
        Object[] args = joinPoint.getArgs();
        ChatRequest chatRequest = (ChatRequest) args[0];
        String message = chatRequest.getMessage();
        Long agentId = chatRequest.getAgentId();
        String modelId = chatRequest.getModelId();
        if (type.equals("message")) {
            if (StrUtil.isEmpty(message) || StrUtil.isEmpty(modelId) || agentId == null || agentId == 0L) {
                throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
            }
        } else if (type.equals("full")) {
            Double temperature = chatRequest.getTemperature();
            Double topP = chatRequest.getTopP();
            Double topK = chatRequest.getTopK();
            Long maxTokens = chatRequest.getMaxTokens();
            if (temperature != null && temperature < 0) {
                throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
            }
            if (topP != null && (topP <= 0 || topP > 1)) {
                throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
            }
            if (topK != null && topK <= 0) {
                throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
            }
            if (maxTokens != null && maxTokens <= 0) {
                throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
            }
        } else {
            throw new AgentException(ResultCodeEnum.PARAMETER_FAIL);
        }
    }
}
