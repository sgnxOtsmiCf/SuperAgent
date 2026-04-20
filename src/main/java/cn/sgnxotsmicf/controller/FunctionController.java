package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ToolVo;
import cn.sgnxotsmicf.service.FunctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 18:30
 * @Version: 1.0
 * @Description: 为前端用户展示后端工具和skills
 */

@RestController
@RequestMapping("/function")
@Tag(name = "工具展示控制器", description = "为前端用户展示后端工具和skills")
public class FunctionController {

    @Resource
    private FunctionService functionService;


    @Operation(summary = "为前端展示当前拥有的工具:通用+online")
    @GetMapping("/tool")
    @SaCheckLogin
    public Result<List<ToolVo>> getFunction() {
        return functionService.getFunction();
    }


    @Operation(summary = "为前端展示当前拥有的skills")
    @GetMapping("/skills")
    @SaCheckLogin
    public Result<List<ToolVo>> getSkills() {
        return functionService.getSkills();
    }

}
