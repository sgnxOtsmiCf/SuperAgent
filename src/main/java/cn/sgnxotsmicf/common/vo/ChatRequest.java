package cn.sgnxotsmicf.common.vo;

import lombok.Data;


@Data
public class ChatRequest {

    private Long agentId;

    private String message;

    private String sessionId;

    private String chatId;

    /**
     * 模型ID，用于指定使用的AI模型
     * 支持：qwen-plus、qwen-max、qwen-turbo、deepseek-chat、deepseek-reasoner、glm-4、glm-4-plus 等
     * agent=2和3时，默认不填即为 qwen-plus
     */
    private String modelId;

    private Double temperature;

    private Double topP;

    /**
     * 有些模型可能不支持
     */
    private Double topK;

    private Long maxTokens;


    private Long thinkingBudget;

    /**
     * 有些模型可能不支持
     */
    private Boolean enableThinking;

    /**
     * 有些模型可能不支持
     */
    private Boolean enableSearch;
}