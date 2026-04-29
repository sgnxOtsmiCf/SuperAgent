package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AuthEnum {

    //bearer, api_key, oauth2
    BEARER("bearer"),
    API_KEY("api_key"),
    OAUTH2("oauth2")
    ;

    @EnumValue
    private final String type;

    AuthEnum(String type) {
        this.type = type;
    }

    public static AuthEnum getByType(String type) {
        for (AuthEnum authEnum : AuthEnum.values()) {
            if (authEnum.type.equals(type)) {
                return authEnum;
            }
        }
        return null;
    }
}
