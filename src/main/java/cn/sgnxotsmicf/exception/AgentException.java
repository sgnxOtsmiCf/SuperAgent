package cn.sgnxotsmicf.exception;

import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/29 16:29
 * @Version: 1.0
 * @Description:
 */

@Hidden
@EqualsAndHashCode(callSuper = true)
@Data
public class AgentException extends RuntimeException {

    private Integer code;
    private String message;
    private ResultCodeEnum resultCodeEnum;


    public AgentException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public AgentException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    public AgentException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();
        this.resultCodeEnum = resultCodeEnum;
    }
    public AgentException(ResultCodeEnum resultCodeEnum, Throwable cause) {
        super(resultCodeEnum.getMessage(), cause);
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();
        this.resultCodeEnum = resultCodeEnum;
    }

}
