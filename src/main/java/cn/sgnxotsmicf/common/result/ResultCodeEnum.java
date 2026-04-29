package cn.sgnxotsmicf.common.result;

import lombok.Getter;

/**
 * 统一返回结果状态信息类
 *
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200,"成功"),
    FAIL(201, "失败"),
    SERVICE_ERROR(202, "服务异常"),
    DATA_ERROR(204, "数据异常"),
    ILLEGAL_REQUEST(205, "非法请求"),
    REPEAT_SUBMIT(206, "重复提交"),
    ARGUMENT_VALID_ERROR(210, "参数校验异常"),
    SIGN_ERROR(300, "签名错误"),
    SIGN_OVERDUE(301, "签名已过期"),

    LOGIN_AUTH(208, "未登陆"),
    PERMISSION(209, "没有权限"),
    ACCOUNT_ERROR(214, "用户名不正确"),
    PASSWORD_ERROR(215, "密码不正确"),
    PHONE_CODE_ERROR(215, "手机验证码不正确"),
    ACCOUNT_REPEAT( 216, "用户名重复"),
    ACCOUNT_STOP( 217, "账号已停用"),
    PHONE_FALSE( 218, "手机号不合法"),
    GET_USERID_FAIL( 219, "获取用户id失败"),
    SESSION_ID_NO_EXITS( 220, "当前用户的此agent不存在此会话id或会话id不合法"),
    AGENT_SESSION_EMPTY(221,"Cannot run agent with empty session id"),
    AGENT_ID_NO_EXIST(221,"agentId不存在"),
    SESSION_ID_EXPIRED(222,"sessionId 已经过期，会话自动销毁"),
    SESSION_NO_FIND(223,"会话没有发现，或已经删除"),
    SESSION_ARCHIVE(224,"会话已归档"),
    SESSION_DELETE(225,"会话删除失败"),
    PHONE_REPEAT( 218, "手机号已存在"),

    MESSAGE_NO_FIND(250,"消息不存在，或没有归档"),
    MESSAGE_DELETE_FAIL(251,"消息删除失败"),

    FILE_UPLOAD_FAIL(501,"minio用户图像上传失败"),
    FILE_GET_URL_FAIL(502,"minio获取地址失败"),
    FILE_DOWNLOAD_FAIL(503,"minio获取地址失败"),
    FILE_DELETE_FAIL(504,"minio删除文件失败"),
    BUCKET_SELECT_FAIL(505,"minio查询桶失败"),
    BUCKET_CREATE_FAIL(506,"minio创建失败"),
    AGENT_FAIL(1001,"agent执行异常"),
    MODEL_FAIL(1002,"模型网络连接不稳定,请稍后重试"),
    MODEL_CONFIG(1003,"模型配置不存在"),
    PARAMETER_FAIL(4,"请求参数异常"),

    CAPTCHA_EMPTY(401,"验证码不能为空"),
    CAPTCHA_EXPIRED(402,"验证码已过期"),
    CAPTCHA_ERROR(403,"验证码错误"),
    CAPTCHA_GENERATE_FAIL(404,"验证码生成失败")
    ;

    private final Integer code;

    private final String message;

    private ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
