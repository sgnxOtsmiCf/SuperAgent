package cn.sgnxotsmicf.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 13:53
 * @Version: 1.0
 * @Description:
 */

public class RewriteQueryTransformerFactory {


    /**
     * 预检索|重写:使用大模型对用户的原始查询进行改写，使其更加清晰和详细
     * @return RewriteQueryTransformer bean
     */
    public static RewriteQueryTransformer createRewriteQueryTransformer(ChatModel chatModel) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }


    public static String doQueryRewrite(ChatModel chatModel, String prompt) {
        Query query = new Query(prompt);
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build().transform(query).text();
    }

    public static String doQueryRewrite(ChatModel chatModel, PromptTemplate prompt) {
        Query query = new Query(prompt.getTemplate());
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build().transform(query).text();
    }

    public static String doQueryRewrite(RewriteQueryTransformer rewriteQueryTransformer, Query query) {
        return rewriteQueryTransformer.transform(query).text();
    }

}
