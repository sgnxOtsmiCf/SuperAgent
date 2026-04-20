package cn.sgnxotsmicf.common.tools.email;

// 邮件发送请求（包含所有必要参数）
public record EmailRequest(
    String to,       // 收件人邮箱（必填）
    String subject,  // 邮件主题（必填）
    String content,  // 邮件内容（必填）
    boolean isHtml   // 是否HTML格式（默认true）
){
    // 默认构造器（无isHtml时默认为HTML）
    public EmailRequest(String to, String subject, String content) {
        this(to, subject, content, true);
    }
}