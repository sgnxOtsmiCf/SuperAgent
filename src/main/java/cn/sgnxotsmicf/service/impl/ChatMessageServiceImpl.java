package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.common.po.ChatMessage;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.SessionIdUtil;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.dao.ChatMessageMapper;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
import cn.sgnxotsmicf.service.ChatMessageService;
import cn.sgnxotsmicf.service.ChatSessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 12:25
 * @Version: 1.0
 * @Description:
 */
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {


    private final ChatSessionMapper chatSessionMapper;

    @Override
    public Result<ChatMessageVo> getMessageByMessageId(String sessionId, Long messageId) {
        long flag = chatSessionMapper.selectCount(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId).eq(ChatSession::getSessionStatus, 0));
        if (flag == 0) {
            return Result.build(ResultCodeEnum.MESSAGE_NO_FIND);
        }
        ChatMessage chatMessage = getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getId, messageId)
                .eq(ChatMessage::getIsDeleted, 0));
        ChatMessageVo chatMessageVo = new ChatMessageVo();
        BeanUtils.copyProperties(chatMessage, chatMessageVo);
        return Result.ok(chatMessageVo);
    }

    @Override
    public Result<String> deleteMessageByMessageId(String sessionId, Long messageId) {
        long flag = chatSessionMapper.selectCount(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId).eq(ChatSession::getSessionStatus, 0));
        if (flag == 0) {
            return Result.build(ResultCodeEnum.MESSAGE_NO_FIND);
        }
        boolean status = remove(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getId, messageId));
        if (!status) {
            return Result.build(ResultCodeEnum.MESSAGE_DELETE_FAIL);
        }
        return Result.ok();
    }


    @Override
    public Result<String> deleteBatchMessageByMessageId(String sessionId, List<Long> messageIds) {
        long flag = chatSessionMapper.selectCount(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId).eq(ChatSession::getSessionStatus, 0));
        if (flag == 0) {
            return Result.build(ResultCodeEnum.MESSAGE_NO_FIND);
        }
        boolean status = remove(new LambdaQueryWrapper<ChatMessage>().in(ChatMessage::getId, messageIds));
        if (!status) {
            return Result.build(ResultCodeEnum.MESSAGE_DELETE_FAIL);
        }
        return Result.ok();
    }
}
