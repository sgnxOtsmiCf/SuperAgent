package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.SessionArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/7 14:40
 * @Version: 1.0
 * @Description:
 */
@RestController
@RequestMapping("/session")
@Tag(name = "会话管理")
public class SessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private SessionArchiveService sessionArchiveService;

    @Operation(summary = "查询当前用户的所有会话")
    @SaCheckLogin
    @GetMapping
    public Result<List<ChatSessionVo>> getSessions(@RequestParam("agentId") Long agentId) {
        return chatSessionService.getSessions(agentId);
    }

    @Operation(summary = "查询当前用户的分页会话")
    @SaCheckLogin
    @GetMapping("/page")
    public Result<List<ChatSessionVo>> getSessionsPage(@RequestParam("agentId") Long agentId,
                                                       @RequestParam("pageNo") Integer pageNo,
                                                       @RequestParam("pageSize") Integer pageSize) {
        return chatSessionService.getSessionsPage(agentId, pageNo, pageSize);
    }

    @Operation(summary = "根据会话id查询会话记录")
    @SaCheckLogin
    @GetMapping("/single")
    public Result<ChatSessionVo> getSessionBySessionId(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId) {
        return chatSessionService.getSessionBySessionId(agentId, sessionId);
    }

    @Operation(summary = "根据会话id删除会话记录")
    @SaCheckLogin
    @DeleteMapping
    public Result<String> deleteSessionById(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId) {
        return chatSessionService.deleteSessionById(agentId, sessionId);
    }

    @Operation(summary = "修改会话标题名称")
    @SaCheckLogin
    @PostMapping
    public Result<String> setNameBySessionId(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId, @RequestParam("sessionName") String sessionName) {
        return chatSessionService.setNameBySessionId(agentId, sessionId, sessionName);
    }

    @Operation(summary = "归档session会话(异步归档到MySQL)")
    @SaCheckLogin
    @PostMapping("/archive")
    public Result<String> archiveSession(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId) {
        return sessionArchiveService.sendArchiveMessage(sessionId, agentId);
    }

    @Operation(summary = "查询会话归档状态")
    @SaCheckLogin
    @GetMapping("/archive/status")
    public Result<Boolean> isSessionArchived(@RequestParam("sessionId") String sessionId) {
        return Result.ok(sessionArchiveService.isSessionArchived(sessionId));
    }

    @SaCheckLogin
    @Operation(summary = "分页查询归档数据")
    @GetMapping("/archive")
    public Result<List<ChatSessionVo>> getArchiveSessionPage(@RequestParam("agentId") Long agentId, @RequestParam("pageNo") Integer pageNo, @RequestParam("pageSize") Integer pageSize) {
        return Result.ok(sessionArchiveService.getArchiveSessionPage(agentId, pageNo, pageSize));
    }


    @SaCheckLogin
    @Operation(summary = "置顶会话数据")
    @GetMapping("/isTop")
    public Result<String> setTopSession(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId) {
        return chatSessionService.setTopSession(agentId, sessionId);
    }

    @SaCheckLogin
    @Operation(summary = "取消置顶会话数据")
    @GetMapping("/isUpTop")
    public Result<String> setUpTopSession(@RequestParam("agentId") Long agentId, @RequestParam("sessionId") String sessionId) {
        return chatSessionService.setUpTopSession(agentId, sessionId);
    }

}
