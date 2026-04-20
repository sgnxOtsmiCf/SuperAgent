package cn.sgnxotsmicf.agentTool.onlinetool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/26 17:18
 * @Version: 1.0
 * @Description: 获取用户时区的当前日期和时间
 */

@Component
public class DateTimeTool {

    @Tool(description = "Get the current date and time in the system's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}
