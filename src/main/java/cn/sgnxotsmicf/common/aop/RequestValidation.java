package cn.sgnxotsmicf.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD})
public @interface RequestValidation {

    /**
     * 校验请求参数ChatRequest是否合法,三种类型
     * "message":校验基本的输入请求是否合法
     * "full":所有的参数(除了sessionId和chatId)是否合法
     */
    String type() default "message";

}
