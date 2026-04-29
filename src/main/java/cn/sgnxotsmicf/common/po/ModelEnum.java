package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ModelEnum {

    LLM("llm"),
    EMBEDDING("embedding"),
    IMAGE("image"),
    AUDIO("audio"),
    MULTIMODAL("multimodal"),
    ;

    @EnumValue
    private final String type;

    //llm, embedding, image, audio, multimodal
    ModelEnum(String type) {
        this.type = type;
    }

    public static ModelEnum getByType(String type) {
        for (ModelEnum modelEnum : ModelEnum.values()) {
            if (modelEnum.type.equals(type)) {
                return modelEnum;
            }
        }
        return null;
    }
}
