package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.sgnxotsmicf.common.po.ModelConfig;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ModelGroupVo;
import cn.sgnxotsmicf.common.vo.ModelProviderVo;
import cn.sgnxotsmicf.common.vo.ModelVo;
import cn.sgnxotsmicf.service.ModelAndTokenService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 21:56
 * @Version: 1.0
 * @Description: 模型和用量管理
 */

@RestController
@RequestMapping("/model")
@Tag(name = "模型和用量控制器", description = "模型和用量管理")
@RequiredArgsConstructor
public class ModelAndTokenController {


    private final ModelAndTokenService modelAndTokenService;

    @SaCheckLogin
    @Operation(summary = "获取用户模型配置")
    @GetMapping("/config")
    public Result<ModelConfig> getModelConfig() {
        return modelAndTokenService.getModelConfig();
    }

    @SaCheckLogin
    @Operation(summary = "保存用户模型配置")
    @PostMapping("/config")
    public Result<String> saveModelConfig(@RequestBody ModelConfig modelConfig) {
        return modelAndTokenService.saveModelConfig(modelConfig);
    }

    @Operation(summary = "获取可用模型列表(支持分组/供应商/类型/关键字/分页筛选)")
    @GetMapping("/list")
    public Result<IPage<ModelVo>> getModelList(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return modelAndTokenService.getModelList(groupId, providerId, modelType, keyword, pageNo, pageSize);
    }

    @Operation(summary = "获取供应商列表（含下属模型，支持分页）")
    @GetMapping("/providers")
    public Result<IPage<ModelProviderVo>> getModelProviders(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return modelAndTokenService.getModelProviders(pageNo, pageSize);
    }

    @Operation(summary = "获取分组列表（含下属模型，支持分页）")
    @GetMapping("/groups")
    public Result<IPage<ModelGroupVo>> getModelGroups(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return modelAndTokenService.getModelGroups(pageNo, pageSize);
    }

}
