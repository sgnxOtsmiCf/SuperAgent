package cn.sgnxotsmicf.config;


import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 20:32
 * @Version: 1.0
 * @Description:
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        System.out.println("SaTokenConfigure addInterceptors.............");
        registry.addInterceptor(new SaInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        try {
                            return super.preHandle(request, response, handler);
                        } catch (SaTokenContextException e) {
                            // 如果是异步请求时出现Sa-Token上下文未初始化异常，就跳过拦截
                            System.out.println("异步请求中捕获到SaTokenContextException，跳过拦截: " + e.getMessage());
                            return true;
                        }
                    }
                })
                .addPathPatterns("/**")
                .excludePathPatterns("/user/simpleLogin","/user/registerPre","/user/simpleRegister","/user/logout",
                        "/user/LoginWithPhoneCode","/user/LoginWithPhoneCodePre")
                .excludePathPatterns("/error");
        System.out.println("SaTokenConfigure addInterceptors end.............");
    }
}
