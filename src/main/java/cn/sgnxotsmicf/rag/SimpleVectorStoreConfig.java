package cn.sgnxotsmicf.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/20 17:40
 * @Version: 1.0
 * @Description:
 */

@Configuration
public class SimpleVectorStoreConfig {

    @Resource
    private DocumentLoader documentLoader;

    @Resource
    private MetaDataEnricherConfig metaDataEnricherTool;

    @Bean
    public SimpleVectorStore simpleVectorStore(EmbeddingModel dashscopeEmbeddingModel, String ragPath) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        //加载文档---暂时注释掉，不加载文档
//        List<Document> documents = documentLoader.loadMarkDowns(ragPath);
//        List<Document> enrichedDocuments = metaDataEnricherTool.enrichDocuments(documents, 3);
//        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }
}
