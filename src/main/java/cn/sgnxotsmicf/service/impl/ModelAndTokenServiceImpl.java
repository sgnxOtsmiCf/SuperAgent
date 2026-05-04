package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.common.po.ModelConfig;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.ModelGroupVo;
import cn.sgnxotsmicf.common.vo.ModelProviderVo;
import cn.sgnxotsmicf.common.vo.ModelVo;
import cn.sgnxotsmicf.dao.ModelConfigMapper;
import cn.sgnxotsmicf.dao.ModelGroupMapper;
import cn.sgnxotsmicf.dao.ModelMapper;
import cn.sgnxotsmicf.dao.ModelProviderMapper;
import cn.sgnxotsmicf.service.ModelAndTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 9:16
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelAndTokenServiceImpl implements ModelAndTokenService {

    private final ModelMapper modelMapper;

    private final ModelConfigMapper modelConfigMapper;

    private final ModelProviderMapper modelProviderMapper;

    private final ModelGroupMapper modelGroupMapper;

    private final ServiceUtil serviceUtil;

    @Override
    public Result<ModelConfig> getModelConfig() {
        Long userId = serviceUtil.getUserId();
        ModelConfig modelConfig = modelConfigMapper
                .selectOne(
                        new LambdaQueryWrapper<ModelConfig>()
                                .eq(ModelConfig::getUserId, userId)
                                .eq(ModelConfig::getIsDeleted, 0)
                );
        if (Objects.isNull(modelConfig)) {
            return Result.build(ResultCodeEnum.MODEL_CONFIG);
        }
        return Result.ok(modelConfig);
    }

    @Override
    public Result<IPage<ModelVo>> getModelList(Long groupId, Long providerId, String modelType, String keyword, int pageNo, int pageSize) {
        Page<ModelVo> page = new Page<>(pageNo, pageSize);
        IPage<ModelVo> resultPage = modelMapper.selectModelVoPage(page, groupId, providerId, modelType, keyword);
        return Result.ok(resultPage);
    }

    @Override
    public Result<String> saveModelConfig(ModelConfig modelConfig) {
        Long userId = serviceUtil.getUserId();
        modelConfig.setUserId(userId);
        ModelConfig existingConfig = modelConfigMapper
                .selectOne(
                        new LambdaQueryWrapper<ModelConfig>()
                                .eq(ModelConfig::getUserId, userId)
                                .eq(ModelConfig::getIsDeleted, 0)
                );
        if (Objects.nonNull(existingConfig)) {
            modelConfig.setId(existingConfig.getId());
            modelConfigMapper.updateById(modelConfig);
        } else {
            modelConfigMapper.insert(modelConfig);
        }
        return Result.ok("模型配置保存成功");
    }

    @Override
    public Result<IPage<ModelProviderVo>> getModelProviders(int pageNo, int pageSize) {
        Page<ModelProviderVo> page = new Page<>(pageNo, pageSize);
        IPage<ModelProviderVo> resultPage = modelProviderMapper.selectModelProviderVoPage(page);
        return Result.ok(resultPage);
    }

    @Override
    public Result<IPage<ModelGroupVo>> getModelGroups(int pageNo, int pageSize) {
        Page<ModelGroupVo> page = new Page<>(pageNo, pageSize);
        IPage<ModelGroupVo> resultPage = modelGroupMapper.selectModelGroupVoPage(page);
        return Result.ok(resultPage);
    }
}
