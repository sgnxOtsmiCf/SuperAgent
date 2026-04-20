package cn.sgnxotsmicf.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 13:57
 * @Version: 1.0
 * @Description:
 */

public class ContextualQueryAugmenterFactory {


    /**
     * 查询增加:空上下文处理器
     * @return ContextualQueryAugmenter bean
     */
    public static ContextualQueryAugmenter createContextualQueryAugmenter(boolean allowEmptyContext) {
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(allowEmptyContext)
                .build();
    }

    /**
     * 查询增加:空上下文处理器
     * @return ContextualQueryAugmenter bean
     */
    public static ContextualQueryAugmenter createContextualQueryAugmenter(String promptTemplate) {
        PromptTemplate template = new PromptTemplate(promptTemplate);
        return ContextualQueryAugmenter.builder()
                .promptTemplate(template)
                .allowEmptyContext(false)
                .build();
    }

    /**
     * 查询增加:空上下文处理器
     * @return ContextualQueryAugmenter bean
     */
    public static ContextualQueryAugmenter createContextualQueryAugmenter(PromptTemplate promptTemplate) {
        return ContextualQueryAugmenter.builder()
                .promptTemplate(promptTemplate)
                .allowEmptyContext(false)
                .build();
    }
}
