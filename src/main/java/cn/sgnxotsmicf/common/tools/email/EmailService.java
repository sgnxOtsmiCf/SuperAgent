package cn.sgnxotsmicf.common.tools.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final String fromEmail; // 发件人邮箱（从配置读取）

    public EmailService(JavaMailSender javaMailSender, 
                       @Value("${spring.mail.username}") String fromEmail) {
        this.javaMailSender = javaMailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * 发送HTML格式邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content HTML内容
     */
    public void sendHtmlEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true表示HTML格式
        
        javaMailSender.send(message);
    }

    /**
     * 发送纯文本邮件
     */
    public void sendTextEmail(String to, String subject, String content) throws MessagingException {
        sendHtmlEmail(to, subject, "<pre>" + content + "</pre>");
    }
}