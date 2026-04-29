package cn.sgnxotsmicf.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 15:24
 * @Version: 0.1
 * @Description:
 */

public interface ModelCommon {

    // 支持的模型常量定义
    String MODEL_QWEN_PLUS = "qwen-plus";
    String MODEL_QWEN_MAX = "qwen-max";
    String MODEL_QWEN_TURBO = "qwen-turbo";
    String MODEL_DEEPSEEK_CHAT = "deepseek-chat";
    String MODEL_DEEPSEEK_REASONER = "deepseek-reasoner";
    String MODEL_DEEPSEEK_V4_FLASH = "deepseek-v4-flash";
    String MODEL_DEEPSEEK_V4_PRO = "deepseek-v4-pro";
    String MODEL_GLM_4 = "glm-4";
    String MODEL_GLM_4_PLUS = "glm-4-plus";
    String MODEL_GLM_4_FLASH = "glm-4-flash";
    String MODEL_GLM_4_5_AIRX = "glm-4.5-airx";
    String MODEL_GLM_4_6_V = "glm-4.6v";
    String MODEL_GLM_4_7 = "glm-4.7";

    String QWEN_PREFIX = "qwen";
    String GLM_PREFIX = "glm";
    String DEEPSEEK_PREFIX = "deepseek";
    String MINIMAX_PREFIX = "minimax";

    default Map<String, List<String>> getModelMap(){
        Map<String, List<String>> map = new HashMap<>();
        map.put("qwen", List.of(MODEL_QWEN_PLUS, MODEL_QWEN_MAX, MODEL_QWEN_TURBO));
        map.put("deepseek", List.of(MODEL_DEEPSEEK_CHAT, MODEL_DEEPSEEK_REASONER, MODEL_DEEPSEEK_V4_FLASH, MODEL_DEEPSEEK_V4_PRO));
        map.put("glm", List.of(MODEL_GLM_4, MODEL_GLM_4_PLUS, MODEL_GLM_4_FLASH, MODEL_GLM_4_5_AIRX, MODEL_GLM_4_6_V, MODEL_GLM_4_7));
        return map;
    }
}
