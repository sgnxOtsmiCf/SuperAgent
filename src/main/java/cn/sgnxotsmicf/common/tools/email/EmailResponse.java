package cn.sgnxotsmicf.common.tools.email;

// 邮件发送响应
public record EmailResponse(
    boolean success, // 是否发送成功
    String message,  // 结果信息
    String timestamp // 发送时间
) {

}