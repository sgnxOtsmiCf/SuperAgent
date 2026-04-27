package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import cn.sgnxotsmicf.service.OpenManusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * @Author: lixiang
 * @CreateDate: 2026/3/30 20:35
 * @Version: 1.0
 * @Description:
 */

@Tag(name = "Manus控制器")
@RestController
@RequestMapping("/manus/chat")
public class ManusController {

    @Resource
    private OpenManusService openManusService;

    /**
     * 流式调用 Manus 超级智能体
     */
    @SaCheckLogin
    @Operation(summary = "流式调用manus agent")
    @PostMapping("/sse")
    public SseEmitter doChatWithSseEmitter(@RequestBody ChatRequest chatRequest) {
        //这个bug还没有解决，简单做一个拦截判断
        StpUtil.getLoginIdAsLong();
        return openManusService.doChatWithSseEmitter(chatRequest);
    }

}
