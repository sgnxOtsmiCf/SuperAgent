package cn.sgnxotsmicf.chatMemory;

import cn.hutool.json.JSONUtil;
import cn.sgnxotsmicf.common.po.ChatMessage;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
import cn.sgnxotsmicf.service.ChatMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MySQL对话记忆存储
 * 特性：不删历史、纯增量、低DML压力、自动生成会话名
 */
@Component
@RequiredArgsConstructor
public class MySqlChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageService chatMessageService;

    private final ChatSessionMapper chatSessionMapper;


    @NotNull
    @Override
    public List<String> findConversationIds() {
        //字面意思，查询当前用户的所有会话
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        // 设置查询条件（删除原代码重复的 userId 条件）
        wrapper.eq(ChatSession::getSessionStatus, 1)
                .eq(ChatSession::getIsDeleted, 0);
        // Mapper 查询数据 + 流处理提取 sessionId
        return chatSessionMapper.selectList(wrapper)
                .stream()
                .map(ChatSession::getSessionId)
                .collect(Collectors.toList());
    }

    public List<String> findConversationIds(Long UserId, Long agentId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, UserId)
                .eq(ChatSession::getSessionStatus, 1)
                .eq(ChatSession::getIsDeleted, 0)
                .eq(ChatSession::getAgentId, agentId)
                .select(ChatSession::getSessionId)
                .orderByDesc(ChatSession::getSessionId);
        return chatSessionMapper.selectList(wrapper)
                .stream()
                .map(ChatSession::getSessionId)
                .collect(Collectors.toList());
    }

    public List<String> findConversationIdsPage(Long UserId, Integer pageNo, Integer pageSize, Long agentId) {
        IPage<ChatSession> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, UserId)
                .eq(ChatSession::getSessionStatus, 1)
                .eq(ChatSession::getIsDeleted, 0)
                .eq(ChatSession::getAgentId, agentId)
                .select(ChatSession::getSessionId)
                .orderByDesc(ChatSession::getSessionId);
        chatSessionMapper.selectPage(page, wrapper);
        return page.getRecords()
                .stream()
                .map(ChatSession::getSessionId)
                .collect(Collectors.toList());
    }


    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String conversationId) {
        List<ChatMessage> messageList = chatMessageService.lambdaQuery()
                .eq(ChatMessage::getSessionId, conversationId)
                .eq(ChatMessage::getIsDeleted, 0)
                .orderByAsc(ChatMessage::getMessageTime)
                .list();
        return messageList.stream()
                .map(this::convertToSpringAiMessage)
                .collect(Collectors.toList());
    }

    public List<ChatMessage> findByConversationIdToChatMessage(@NotNull String conversationId) {
        return chatMessageService.lambdaQuery()
                .eq(ChatMessage::getSessionId, conversationId)
                .eq(ChatMessage::getIsDeleted, 0)
                .orderByAsc(ChatMessage::getMessageTime)
                .list();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(@NotNull String conversationId, @NotNull List<Message> messages) {
        // 更新会话
        buildChatSession(conversationId);
        // 纯增量插入，无删除无更新，MySQL压力极小
        List<Message> list = new ArrayList<>();
        list.add(messages.getLast());
        List<ChatMessage> poList = convertToChatMessage(list, conversationId);
        chatMessageService.saveBatch(poList);
    }

    @Override
    public void deleteByConversationId(@NotNull String conversationId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, conversationId).set(ChatSession::getSessionStatus, 2);
        chatSessionMapper.update(null, updateWrapper);
        chatMessageService.remove(chatMessageService.lambdaQuery().eq(ChatMessage::getSessionId, conversationId));
    }


    private void buildChatSession(String sessionId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getIsDeleted, 0);
        ChatSession session = chatSessionMapper.selectOne(wrapper);
        session.setLastActive(LocalDateTime.now());
        if (session.getId() != null) {
            chatSessionMapper.updateById(session);
        } else {
            chatSessionMapper.insert(session);
        }
    }

    public Message convertToSpringAiMessage(ChatMessage po) {
        MessageType type = MessageType.valueOf(po.getMessageType());
        return switch (type) {
            case USER -> UserMessage.builder().text(po.getContent()).metadata(po.getMetadata()).build();
            case ASSISTANT -> {
                String toolCalls = (String) po.getMetadata().get("toolCalls");
                List<AssistantMessage.ToolCall> toolCallList = JSONUtil.toList(toolCalls, AssistantMessage.ToolCall.class);
                yield AssistantMessage.builder().toolCalls(toolCallList).content(po.getContent()).properties(po.getMetadata()).build();
            }
            case SYSTEM -> SystemMessage.builder().text(po.getContent()).metadata(po.getMetadata()).build();
            case TOOL -> ToolResponseMessage.builder().metadata(po.getMetadata())
                    .responses(JSONUtil.toList(po.getContent(), ToolResponseMessage.ToolResponse.class)).build();
        };
    }

    private List<ChatMessage> convertToChatMessage(List<Message> poList, String conversationId) {
        return poList.stream().map(po -> {
            MessageType type = po.getMessageType();
            ChatMessage message = new ChatMessage();
            message.setSessionId(conversationId);
            message.setMessageType(po.getMessageType().name());
            message.setContent(po.getText());
            message.setMessageTime(LocalDateTime.now());
            switch (type) {
                case USER -> message.setMetadata(po.getMetadata());
                case ASSISTANT -> {
                    AssistantMessage assistantMessage = (AssistantMessage) po;
                    List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
                    Map<String, Object> metadata = assistantMessage.getMetadata();
                    metadata.put("toolCalls", toolCalls);
                    message.setMetadata(metadata);
                }
            }
            return message;
        }).collect(Collectors.toList());
    }
}