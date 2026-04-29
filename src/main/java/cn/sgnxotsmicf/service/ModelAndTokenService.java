package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.po.ModelConfig;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ModelGroupVo;
import cn.sgnxotsmicf.common.vo.ModelProviderVo;
import cn.sgnxotsmicf.common.vo.ModelVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 22:55
 * @Version: 1.0
 * @Description:
 */

public interface ModelAndTokenService {

    Result<ModelConfig> getModelConfig();

    Result<IPage<ModelVo>> getModelList(Long groupId, Long providerId, String modelType, String keyword, int pageNo, int pageSize);

    Result<String> saveModelConfig(ModelConfig modelConfig);

    Result<IPage<ModelProviderVo>> getModelProviders(int pageNo, int pageSize);

    Result<IPage<ModelGroupVo>> getModelGroups(int pageNo, int pageSize);
}
