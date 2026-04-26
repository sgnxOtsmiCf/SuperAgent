package cn.sgnxotsmicf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.sgnxotsmicf.common.rabbitmq.entity.MqFailMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MqFailMessageMapper extends BaseMapper<MqFailMessage> {
}