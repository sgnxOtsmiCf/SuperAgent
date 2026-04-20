package cn.sgnxotsmicf.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/2 15:43
 * @Version: 1.0
 * @Description:
 */

public class CompressionQueryTransformerFactory {

    public static Query createCompressionQueryTransformer(ChatClient.Builder chatClient, Query query) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClient)
                .build().transform(query);
    }
}
