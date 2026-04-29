package cn.sgnxotsmicf.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.sgnxotsmicf.common.po.Model;
import cn.sgnxotsmicf.common.po.ModelConfig;
import cn.sgnxotsmicf.common.po.ModelEnum;
import cn.sgnxotsmicf.common.po.ModelGroup;
import cn.sgnxotsmicf.common.po.ModelGroupRelation;
import cn.sgnxotsmicf.common.po.ModelProvider;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.ModelGroupVo;
import cn.sgnxotsmicf.common.vo.ModelProviderVo;
import cn.sgnxotsmicf.common.vo.ModelVo;
import cn.sgnxotsmicf.dao.ModelConfigMapper;
import cn.sgnxotsmicf.dao.ModelGroupMapper;
import cn.sgnxotsmicf.dao.ModelGroupRelationMapper;
import cn.sgnxotsmicf.dao.ModelMapper;
import cn.sgnxotsmicf.dao.ModelProviderMapper;
import cn.sgnxotsmicf.service.ModelAndTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final ModelGroupRelationMapper modelGroupRelationMapper;

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
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<Model>()
                .eq(Model::getStatus, 1)
                .eq(Model::getIsDeleted, 0);

        if (Objects.nonNull(groupId)) {
            List<Long> modelIds = modelGroupRelationMapper.selectList(
                    new LambdaQueryWrapper<ModelGroupRelation>()
                            .eq(ModelGroupRelation::getGroupId, groupId)
                            .eq(ModelGroupRelation::getIsDeleted, 0)
            ).stream().map(ModelGroupRelation::getModelId).collect(Collectors.toList());
            if (modelIds.isEmpty()) {
                Page<ModelVo> emptyPage = new Page<>(pageNo, pageSize);
                emptyPage.setTotal(0);
                return Result.ok(emptyPage);
            }
            wrapper.in(Model::getId, modelIds);
        }
        if (Objects.nonNull(providerId)) {
            wrapper.eq(Model::getProviderId, providerId);
        }
        if (StrUtil.isNotBlank(modelType)) {
            ModelEnum typeEnum = ModelEnum.getByType(modelType);
            if (typeEnum != null) {
                wrapper.eq(Model::getModelType, typeEnum);
            }
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Model::getModelName, keyword)
                    .or().like(Model::getModelCode, keyword));
        }

        wrapper.orderByDesc(Model::getIsRecommended)
                .orderByAsc(Model::getSortOrder);

        Page<Model> page = new Page<>(pageNo, pageSize);
        IPage<Model> modelPage = modelMapper.selectPage(page, wrapper);

        List<ModelVo> records = modelPage.getRecords().stream().map(model -> {
            ModelVo modelVo = new ModelVo();
            BeanUtils.copyProperties(model, modelVo);
            return modelVo;
        }).collect(Collectors.toList());

        Page<ModelVo> resultPage = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        resultPage.setRecords(records);
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
        Page<ModelProvider> page = new Page<>(pageNo, pageSize);
        IPage<ModelProvider> providerPage = modelProviderMapper.selectPage(page,
                new LambdaQueryWrapper<ModelProvider>()
                        .eq(ModelProvider::getStatus, 1)
                        .eq(ModelProvider::getIsDeleted, 0)
                        .orderByAsc(ModelProvider::getPriority)
        );
        List<ModelProvider> providers = providerPage.getRecords();
        List<Long> providerIds = providers.stream().map(ModelProvider::getId).collect(Collectors.toList());
        Map<Long, List<ModelVo>> providerModelMap = Collections.emptyMap();
        if (!providerIds.isEmpty()) {
            List<Model> allModels = modelMapper.selectList(
                    new LambdaQueryWrapper<Model>()
                            .in(Model::getProviderId, providerIds)
                            .eq(Model::getStatus, 1)
                            .eq(Model::getIsDeleted, 0)
                            .orderByDesc(Model::getIsRecommended)
                            .orderByAsc(Model::getSortOrder)
            );
            providerModelMap = allModels.stream().map(m -> {
                ModelVo vo = new ModelVo();
                BeanUtils.copyProperties(m, vo);
                return vo;
            }).collect(Collectors.groupingBy(ModelVo::getProviderId));
        }
        Map<Long, List<ModelVo>> finalMap = providerModelMap;
        List<ModelProviderVo> records = providers.stream().map(p -> {
            ModelProviderVo vo = new ModelProviderVo();
            BeanUtils.copyProperties(p, vo);
            vo.setModelVoList(finalMap.getOrDefault(p.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        Page<ModelProviderVo> resultPage = new Page<>(providerPage.getCurrent(), providerPage.getSize(), providerPage.getTotal());
        resultPage.setRecords(records);
        return Result.ok(resultPage);
    }

    @Override
    public Result<IPage<ModelGroupVo>> getModelGroups(int pageNo, int pageSize) {
        Page<ModelGroup> page = new Page<>(pageNo, pageSize);
        IPage<ModelGroup> groupPage = modelGroupMapper.selectPage(page,
                new LambdaQueryWrapper<ModelGroup>()
                        .eq(ModelGroup::getStatus, 1)
                        .eq(ModelGroup::getIsDeleted, 0)
                        .orderByAsc(ModelGroup::getSortOrder)
        );
        List<ModelGroup> groups = groupPage.getRecords();
        List<Long> groupIds = groups.stream().map(ModelGroup::getId).collect(Collectors.toList());
        Map<Long, List<Long>> groupModelIdMap = Collections.emptyMap();
        if (!groupIds.isEmpty()) {
            List<ModelGroupRelation> relations = modelGroupRelationMapper.selectList(
                    new LambdaQueryWrapper<ModelGroupRelation>()
                            .in(ModelGroupRelation::getGroupId, groupIds)
                            .eq(ModelGroupRelation::getIsDeleted, 0)
            );
            groupModelIdMap = relations.stream().collect(
                    Collectors.groupingBy(ModelGroupRelation::getGroupId,
                            Collectors.mapping(ModelGroupRelation::getModelId, Collectors.toList()))
            );
        }
        List<Long> allModelIds = groupModelIdMap.values().stream()
                .flatMap(List::stream).distinct().collect(Collectors.toList());
        Map<Long, ModelVo> modelVoMap = Collections.emptyMap();
        if (!allModelIds.isEmpty()) {
            modelVoMap = modelMapper.selectList(
                    new LambdaQueryWrapper<Model>()
                            .in(Model::getId, allModelIds)
                            .eq(Model::getStatus, 1)
                            .eq(Model::getIsDeleted, 0)
                            .orderByDesc(Model::getIsRecommended)
                            .orderByAsc(Model::getSortOrder)
            ).stream().map(m -> {
                ModelVo vo = new ModelVo();
                BeanUtils.copyProperties(m, vo);
                return vo;
            }).collect(Collectors.toMap(ModelVo::getId, v -> v, (a, b) -> a));
        }
        Map<Long, List<Long>> finalGroupModelIdMap = groupModelIdMap;
        Map<Long, ModelVo> finalModelVoMap = modelVoMap;
        List<ModelGroupVo> records = groups.stream().map(g -> {
            ModelGroupVo vo = ModelGroupVo.builder()
                    .id(g.getId())
                    .groupName(g.getGroupName())
                    .groupCode(g.getGroupCode())
                    .sortOrder(g.getSortOrder())
                    .build();
            List<Long> modelIds = finalGroupModelIdMap.getOrDefault(g.getId(), Collections.emptyList());
            List<ModelVo> models = modelIds.stream()
                    .map(finalModelVoMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            vo.setModelVoList(models);
            return vo;
        }).collect(Collectors.toList());

        Page<ModelGroupVo> resultPage = new Page<>(groupPage.getCurrent(), groupPage.getSize(), groupPage.getTotal());
        resultPage.setRecords(records);
        return Result.ok(resultPage);
    }
}
