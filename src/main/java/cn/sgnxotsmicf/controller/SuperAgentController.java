package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.aop.RequestValidation;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import cn.sgnxotsmicf.service.SuperAgentService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 16:58
 * @Version: 1.0
 * @Description:
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/superagent/chat")
public class SuperAgentController {

    @Resource
    private SuperAgentService superAgentService;

    // 从配置文件读取：文件大小限制、允许的格式
    @Value("${ai.upload.max-size:10485760}")
    private long maxFileSize;
    @Value("${ai.upload.allow-types:image/png,image/jpeg,image/jpg,image/webp,audio/mp3,audio/wav,video/mp4}")
    private List<String> allowTypes;

    @SaCheckLogin
    //@SaCheckPermission("agent:super:use")
    @PostMapping("/text/stream")
    @RequestValidation(type = "full")
    public SseEmitter doChatStream(@RequestBody ChatRequest request) {
        return superAgentService.doChatStream(request);
    }

}
