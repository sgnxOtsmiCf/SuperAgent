package cn.sgnxotsmicf.common.version;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:32
 * @Version: 1.0
 * @Description:
 */


@Component
@Slf4j
public class SuperAgentDeficiency {

    @Resource
    private SuperAgentVersion superAgentVersion;

    private final Map<String, String> deficiency = Map.of(
            "前端框架流式markdown渲染基本成功，但还有点不足","ai和我的能力问题，前端知识储备太少",
            "更多细节不足","各种细节包括提示词等都有待提高，目前只是搭了这么一个框架",
            "更多功能不足","包括自定义温度、选择模型等，进行了设计，但还么有具体深入的实装",
            "Rag流程不足","只是添加了一个流程，但是没有具体进行更深层次的架构设计,之后第一个重心",
            "鉴权验证不足","正在逐步优化中，之后第二个重心"
    );

    public Map<String, String> getDeficiency() {
        return deficiency;
    }


}
