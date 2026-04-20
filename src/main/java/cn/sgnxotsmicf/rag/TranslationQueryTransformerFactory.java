package cn.sgnxotsmicf.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 14:07
 * @Version: 1.0
 * @Description:
 */

public class TranslationQueryTransformerFactory {

    /**
     * 将查询翻译成嵌入模型支持的目标语言,如果查询已经是目标语言了,则保持不变.
     * @param chatModel 大模型
     * @param targetLanguage 目标语言
     * @return TranslationQueryTransformer bean
     */
    public static TranslationQueryTransformer createTranslationQueryTransformer(ChatModel chatModel,String targetLanguage) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .targetLanguage(targetLanguage)
                .build();
    }

    /**
     * 将查询翻译成嵌入模型支持的目标语言,如果查询已经是目标语言了,则保持不变.-默认是转成中文
     * @param chatModel 大模型
     * @return TranslationQueryTransformer bean
     */
    public static TranslationQueryTransformer createTranslationQueryTransformer(ChatModel chatModel) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .targetLanguage("chinese")
                .build();
    }

}
