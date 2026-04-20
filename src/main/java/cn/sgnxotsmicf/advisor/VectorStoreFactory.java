package cn.sgnxotsmicf.advisor;

import cn.sgnxotsmicf.rag.ContextualQueryAugmenterFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 14:20
 * @Version: 1.0
 * @Description:
 */

public class VectorStoreFactory {


    public static VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.6)
                .topK(5)
                .build();
    }

    public static VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(VectorStore vectorStore, Filter.Expression filter) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.6)
                .topK(5)
                .filterExpression(filter)
                .build();
    }


    public static VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(VectorStore vectorStore, Double similarityThreshold, int topK) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();
    }

    public static VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(VectorStore vectorStore, Double similarityThreshold, int topK, Filter.Expression filter) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .filterExpression(filter)
                .build();
    }


    public static Advisor createPGVectorAdvisor(VectorStore vectorStore) {
        VectorStoreDocumentRetriever vectorStoreDocumentRetriever =
                VectorStoreFactory.createVectorStoreDocumentRetriever(vectorStore);
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .queryAugmenter(ContextualQueryAugmenterFactory.createContextualQueryAugmenter(true))
                .build();
    }

    public static Advisor createPGVectorAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .order(-1) //比重排模型快即可
                .documentRetriever(vectorStoreDocumentRetriever)
                .queryAugmenter(ContextualQueryAugmenterFactory.createContextualQueryAugmenter(true))
                .build();
    }


}
