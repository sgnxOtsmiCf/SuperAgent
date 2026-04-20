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


@RestController
@RequestMapping("/familyHarmony/chat")
@Tag(name = "家庭和睦ai控制器")
public class FamilyHarmonyController {


    @Resource
    private OpenManusService openManusService;

    /**
     * 流式调用 FamilyHarmony 超级智能体
     * @param chatRequest 请求
     * @return SseEmitter
     */
    //@SaCheckLogin
    @PostMapping("/sse")
    @Operation(summary = "流式调用 FamilyHarmony 超级智能体")
    public SseEmitter doChatWithSseEmitter(@RequestBody ChatRequest chatRequest) {
        //这个bug还没有解决，简单做一个拦截判断
        StpUtil.getLoginIdAsLong();
        return openManusService.doChatWithSseEmitter(chatRequest);
    }
}

