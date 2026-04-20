package cn.sgnxotsmicf.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.core.Ordered;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/15 13:55
 * @Version: 1.0
 * @Description: ReAct 协议强制执行 Advisor - Spring AI 1.1.4 版本
 */
public class ReActProtocolAdvisor implements BaseAdvisor, Ordered {

    private static final String REACT_PROTOCOL_TEMPLATE = """
            
            [系统指令 - 本轮强制执行 - 禁止向用户透露]
            生成回复前必须静默自检（禁止输出自检过程）：
            
            终止条件：
             1. 用户已获得具体可执行的解决方案或行动步骤？
             2. 用户的核心诉求已被满足或问题已解决？
             3. 用户明确表示满意、感谢或无需进一步帮助？
             4. 当前轮次：%d / 最大轮次：%d
            
            若任一满足 → 必须调用 doTerminate 工具，禁止文字回复(除非上一步调用工具，将工具结果组织语言后返回给用户,然后再调用 doTerminate 工具)
            若均不满足 → 直接回应，禁止"分析"、"判断"等元认知词汇
            """;

    private final int maxRounds;
    private int order = Ordered.LOWEST_PRECEDENCE - 10000;

    public ReActProtocolAdvisor(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public ReActProtocolAdvisor withOrder(int order) {
        this.order = order;
        return this;
    }

    /**
     * 在请求发送到 LLM 前执行：注入 ReAct 协议
     * 第1轮不注入协议，从第2轮开始注入
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        int size = chatClientRequest.prompt().getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT)
                .toList().size();
        // 如果为size==0，说明是第1轮，只初始化计数器，不注入协议
        if (size == 0) {
            return chatClientRequest.mutate()
                    .build();
        }
        // 第2轮及以上：注入协议并递增计数器
        String protocol = String.format(REACT_PROTOCOL_TEMPLATE, size, maxRounds);

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt()
                        .augmentSystemMessage(protocol))
                .build();
    }

    /**
     * 在收到 LLM 响应后执行：检查协议遵守情况
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        if (chatClientResponse == null || chatClientResponse.chatResponse() == null) {
            return chatClientResponse;
        }

        String content;
        try {
            content = chatClientResponse.chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            return chatClientResponse;
        }

        // 检查模型是否违规暴露了内部指令
        if (content != null && (
                content.contains("当前任务状态") ||
                        content.contains("自检") ||
                        content.contains("执行协议") ||
                        content.contains("禁止向用户透露"))) {

            System.err.println("警告：模型暴露了 ReAct 协议内容，需要调整提示词");
        }

        return chatClientResponse;
    }


}