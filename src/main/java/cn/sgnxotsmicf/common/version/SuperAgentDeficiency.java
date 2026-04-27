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
            "流式输出推理缺陷","无法将推理内容保存的memory中，感觉是框架缺陷",
            "流式输出工具缺陷","有时候明明调用并执行了工具，但是却没有展示出来，memory中也没有存储",
            "推理阻塞","接着上一个问题，推理的时候，阻塞工具执行(其实执行了)，但是就是无法显示，导致工具存储失败",
            "前端框架markdown渲染不完全","ai和我的能力问题，前端知识储备太少",
            "更多细节不足","各种细节包括提示词等都有待提高，目前只是搭了这么一个框架",
            "更多功能不足","包括自定义温度、选择模型等，都没有设计"
    );

    public Map<String, String> getDeficiency() {
        return deficiency;
    }


}
