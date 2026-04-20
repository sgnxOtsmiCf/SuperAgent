package cn.sgnxotsmicf.controller;

import cn.sgnxotsmicf.common.dto.CaptchaDTO;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:05
 * @Version: 1.0
 * @Description:
 */

@RestController
@RequestMapping("/captcha")
@Tag(name = "验证码控制器")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Operation(summary = "获取登录验证码图片")
    @GetMapping("/generate")
    public Result<CaptchaDTO> generateCaptcha() {
        return Result.ok(captchaService.generateCaptcha());
    }
}
