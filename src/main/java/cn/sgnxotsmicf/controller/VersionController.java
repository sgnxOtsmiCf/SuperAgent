package cn.sgnxotsmicf.controller;

import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.version.SuperAgentAdvantage;
import cn.sgnxotsmicf.common.version.SuperAgentDeficiency;
import cn.sgnxotsmicf.common.version.SuperAgentDetail;
import cn.sgnxotsmicf.common.version.SuperAgentVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/18 13:53
 * @Version: 1.0
 * @Description: 用于项目描述、版本显示
 */

@Tag(name = "版本控制器",description = "用于项目描述、版本显示")
@RestController
@RequestMapping("/version")
public class VersionController {

    @Resource
    private SuperAgentVersion superAgentVersion;

    @Resource
    private SuperAgentDeficiency superAgentDeficiency;

    @Resource
    private SuperAgentAdvantage superAgentAdvantage;

    @Operation(summary = "项目基本信息")
    @GetMapping
    public Result<SuperAgentVersion> getVersion() {
        return Result.ok(superAgentVersion);
    }

    @Operation(summary = "版本详细信息")
    @GetMapping("/all")
    public Result<SuperAgentDetail> getDetailedVersion() {
        SuperAgentDetail detail = new SuperAgentDetail();
        detail.setVersion(superAgentVersion);
        detail.setAdvantages(superAgentAdvantage.getAdvantage());
        detail.setDeficiencies(superAgentDeficiency.getDeficiency());
        return Result.ok(detail);
    }

}
