package cn.sgnxotsmicf.dao;

import cn.sgnxotsmicf.common.po.ModelGroup;
import cn.sgnxotsmicf.common.vo.ModelGroupVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29
 * @Version: 1.0
 */

public interface ModelGroupMapper extends BaseMapper<ModelGroup> {

    IPage<ModelGroupVo> selectModelGroupVoPage(Page<ModelGroupVo> page);
}
