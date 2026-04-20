package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/16 17:00
 * @Version: 1.0
 * @Description: 因为spring ai alibaba框架的问题，本项目目前只支持归档数据的消息级别控制
 */

@RestController
@RequestMapping("/message")
@Tag(name = "消息管理")
public class MessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @Operation(summary = "根据消息id查询单条数据")
    @SaCheckLogin
    @GetMapping("/single")
    public Result<ChatMessageVo> getMessageByMessageId(@RequestParam(name = "sessionId") String sessionId, @RequestParam(name = "messageId") Long messageId) {
        return chatMessageService.getMessageByMessageId(sessionId, messageId);
    }

    @Operation(summary = "根据消息id删除单条数据")
    @SaCheckLogin
    @DeleteMapping("/single")
    public Result<String> deleteMessageByMessageId(@RequestParam(name = "sessionId") String sessionId, @RequestParam(name = "messageId") Long messageId) {
        return chatMessageService.deleteMessageByMessageId(sessionId, messageId);
    }

    @Operation(summary = "根据消息列表ids批量删除单条数据")
    @SaCheckLogin
    @DeleteMapping
    public Result<String> deleteBatchMessageByMessageId(@RequestParam(name = "sessionId") String sessionId, @RequestParam(name = "messageIds") List<Long> messageIds) {
        return chatMessageService.deleteBatchMessageByMessageId(sessionId, messageIds);
    }
}
