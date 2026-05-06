package cn.sgnxotsmicf.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * @Author: lixiang
 * @CreateDate: 2026/5/6 22:43
 * @Version: 1.0
 * @Description: 暂未实现
 */

public class AgentThreadLocalContextInterceptor implements AsyncHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return AsyncHandlerInterceptor.super.preHandle(request, response, handler);
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AsyncHandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
