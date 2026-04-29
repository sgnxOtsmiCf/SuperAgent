package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ProviderEnum {
    //api, local, proxy
    API("api"),
    LOCAL("local"),
    PROXY("proxy");

    @EnumValue
    private final String type;

    ProviderEnum(String type) {
        this.type = type;
    }

    public static ProviderEnum getByType(String type) {
        for (ProviderEnum providerEnum : ProviderEnum.values()) {
            if (providerEnum.type.equals(type)) {
                return providerEnum;
            }
        }
        return null;
    }
}
