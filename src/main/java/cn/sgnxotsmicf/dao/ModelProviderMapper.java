package cn.sgnxotsmicf.dao;

import cn.sgnxotsmicf.common.po.ModelProvider;
import cn.sgnxotsmicf.common.vo.ModelProviderVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29
 * @Version: 1.0
 */

public interface ModelProviderMapper extends BaseMapper<ModelProvider> {

    IPage<ModelProviderVo> selectModelProviderVoPage(Page<ModelProviderVo> page);
}
