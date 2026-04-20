package cn.sgnxotsmicf.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/29 16:28
 * @Version: 1.0
 * @Description:
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(AgentException.class)
    public Result<String> agentException(AgentException agentException) {
        return Result.build(agentException.getResultCodeEnum());
    }

    /**
     * 捕获：未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<String> handleNotLoginException(NotLoginException e) {
        return Result.build(ResultCodeEnum.LOGIN_AUTH);
    }


    /**
     * 捕获：无权限异常（你当前的报错）
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<String> handleNotPermissionException(NotPermissionException e) {
        // e.getMessage() = 无此权限：agent:super:use
        return Result.fail(e.getMessage());
    }

    /**
     * 兜底：捕获所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(HttpServletRequest request, Exception e) {
        // 判断是否为 SSE 请求（根据 Accept 头或 Content-Type）
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/event-stream")) {
            // 不返回 Result 对象，避免序列化错误；可以记录日志，但不额外输出响应
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        log.error(e.getMessage(), e);
        // 原有返回 Result 的逻辑
        return Result.fail("服务器繁忙，请稍后再试");
    }

}
