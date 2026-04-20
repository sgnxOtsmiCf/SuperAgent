package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ToolVo;

import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 18:34
 * @Version: 1.0
 * @Description:
 */

public interface FunctionService {

    Result<List<ToolVo>> getFunction();

    Result<List<ToolVo>> getSkills();
}
