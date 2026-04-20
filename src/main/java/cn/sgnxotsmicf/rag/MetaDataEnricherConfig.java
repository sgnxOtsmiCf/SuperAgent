package cn.sgnxotsmicf.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 14:27
 * @Version: 1.0
 * @Description:
 */
@Component
public class MetaDataEnricherConfig {

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 为文档Document自动补充元信息，默认补充5条
     * @param documents 文档集合对象
     * @return 添加元信息后的文档集合对象
     */
    public List<Document> enrichDocuments(List<Document> documents) {
        return new KeywordMetadataEnricher(dashscopeChatModel,5).apply(documents);
    }

    /**
     * 为文档Document自动补充元信息
     * @param documents 文档集合对象
     * @param keywordCount 添加元信息文档条数
     * @return 添加元信息后的文档集合对象
     */
    public List<Document> enrichDocuments(List<Document> documents, int keywordCount) {
        return new KeywordMetadataEnricher(dashscopeChatModel, keywordCount).apply(documents);
    }
}
