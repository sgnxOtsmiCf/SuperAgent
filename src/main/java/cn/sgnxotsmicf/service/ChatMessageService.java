package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.po.ChatMessage;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.dao.ChatMessageMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatMessageService extends IService<ChatMessage> {

    Result<ChatMessageVo> getMessageByMessageId(String sessionId, Long messageId);

    Result<String> deleteMessageByMessageId(String sessionId, Long messageId);

    Result<String> deleteBatchMessageByMessageId(String sessionId, List<Long> messageIds);
}