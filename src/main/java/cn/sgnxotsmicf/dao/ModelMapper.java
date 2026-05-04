package cn.sgnxotsmicf.dao;

import cn.sgnxotsmicf.common.po.Model;
import cn.sgnxotsmicf.common.vo.ModelVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 9:17
 * @Version: 1.0
 * @Description:
 */

public interface ModelMapper extends BaseMapper<Model> {

    IPage<ModelVo> selectModelVoPage(Page<ModelVo> page,
                                     @Param("groupId") Long groupId,
                                     @Param("providerId") Long providerId,
                                     @Param("modelType") String modelType,
                                     @Param("keyword") String keyword);

    List<ModelVo> selectModelVoListByProviderId(@Param("providerIds") List<Long> providerIds);

    List<ModelVo> selectModelVoListByModelIds(@Param("modelIds") List<Long> modelIds);
}
