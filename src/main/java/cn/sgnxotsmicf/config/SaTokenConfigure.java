package cn.sgnxotsmicf.config;


import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
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
        registry.addInterceptor(new SaInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        // 核心修复：异步分发阶段直接放行
                        if (request.getDispatcherType() == DispatcherType.ASYNC) {
                            return true;
                        }
                        // 正常鉴权逻辑：仅对 REQUEST 分发生效
                        SaRouter.match("/**")
                                .notMatch(
                                        "/user/login",
                                        "/user/registerPre",
                                        "/user/register",
                                        "/user/logout",
                                        "/user/LoginWithPhoneCodePre",
                                        "/error",
                                        "/captcha/*"
                                )
                                .check(r -> StpUtil.checkLogin());

                        return true;
                    }
                })
                .addPathPatterns("/**");
    }
}
