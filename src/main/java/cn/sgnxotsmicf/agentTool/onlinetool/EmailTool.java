package cn.sgnxotsmicf.agentTool.onlinetool;

import cn.sgnxotsmicf.common.tools.email.EmailResponse;
import cn.sgnxotsmicf.common.tools.email.EmailService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EmailTool {

    private final EmailService emailService;

    public EmailTool(EmailService emailService) {
        this.emailService = emailService;
    }

    @Tool(
            name = "sendEmail",
            // 英文工具描述
            description = "Send an email, supports HTML and plain text formats"
    )
    public EmailResponse sendEmail(
            @ToolParam(description = "Recipient email address, e.g., user@example.com", required = true)
            String to,

            @ToolParam(description = "Email subject/title", required = true)
            String subject,

            @ToolParam(description = "Email body content, supports HTML format", required = true)
            String content,

            @ToolParam(description = "Whether the email is in HTML format, true=HTML, false=plain text, default is true")
            Boolean isHtml
    ) {
        try {
            boolean html = isHtml == null || Boolean.TRUE.equals(isHtml);

            if (html) {
                emailService.sendHtmlEmail(to, subject, content);
            } else {
                emailService.sendTextEmail(to, subject, content);
            }

            return new EmailResponse(
                    true,
                    "Email sent successfully",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return new EmailResponse(
                    false,
                    "Failed to send email: " + e.getMessage(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        }
    }
}