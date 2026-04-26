package cn.sgnxotsmicf.job;

import cn.sgnxotsmicf.common.rabbitmq.entity.ArchiveMessage;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.service.SessionArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话过期自动归档定时任务
 *
 * 符合现有job包规范
 * 定期扫描已过期的会话，触发异步归档
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionExpireArchiveJob {

    private final SessionArchiveService sessionArchiveService;

    /**
     * 每小时执行一次：检查过期会话并触发归档
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void archiveExpiredSessions() {
        log.info("开始执行会话过期归档任务");

        try {
            // 1. 获取已过期的会话列表
            List<String> expiredSessionIds = sessionArchiveService.getExpiredSessions();

            if (expiredSessionIds.isEmpty()) {
                log.info("没有需要归档的过期会话");
                return;
            }

            log.info("发现 {} 个过期会话需要归档", expiredSessionIds.size());

            // 2. 批量发送归档消息
            List<ArchiveMessage> messages = expiredSessionIds.stream()
                    .map(sessionId -> ArchiveMessage.createAutoArchiveMessage(sessionId, null, null))
                    .collect(Collectors.toList());

            Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessages(messages);
            log.info("过期会话归档任务完成: 已发送 {} 条归档消息", messages.size());
            log.info("发送核准:{}", stringResult.getMessage());

        } catch (Exception e) {
            log.error("会话过期归档任务执行失败", e);
        }
    }

    /**
     * 每天执行一次：提醒即将过期的会话
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void notifyExpiringSessions() {
        log.info("开始执行即将过期会话提醒任务");

        try {
            // 获取7天内即将过期的会话
            List<String> expiringSessions = sessionArchiveService.getExpiringSessions(7);

            if (expiringSessions.isEmpty()) {
                log.info("没有即将过期的会话");
                return;
            }

            log.info("发现 {} 个即将过期的会话", expiringSessions.size());

            // TODO: 发送通知给用户（可以通过邮件、短信或站内信）
            for (String sessionId : expiringSessions) {
                log.info("会话即将过期: sessionId={}", sessionId);
            }

        } catch (Exception e) {
            log.error("即将过期会话提醒任务执行失败", e);
        }
    }
}
