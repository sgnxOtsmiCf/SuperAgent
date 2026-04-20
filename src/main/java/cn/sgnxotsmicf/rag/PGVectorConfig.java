package cn.sgnxotsmicf.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/23 15:43
 * @Version: 1.0
 * @Description:
 */

@Configuration
@Slf4j
public class PGVectorConfig {

    @Resource(name = "pgVectorStore")
    private VectorStore vectorStore;

    @Resource
    private DocumentLoader documentLoader;

    @Resource
    private MetaDataEnricherConfig metaDataEnricherTool;


    public void loadDocumentToPGVector(String ragPath) {
        List<Document> documents = documentLoader.loadMarkDowns(ragPath);
        List<Document> enrichedDocuments = metaDataEnricherTool.enrichDocuments(documents, 3);
        // 1. 定义分片大小 严格≤10 (阿里云嵌入模型限制)
        int batchSize = 10;
        // 2. 拆分文档列表为多个小批次
        List<List<Document>> batches = splitDocuments(enrichedDocuments, batchSize);

        // 3. 分批插入向量库
        for (List<Document> batch : batches) {
            try {
                vectorStore.add(batch);
                log.info("成功插入批次,数量:{}", batch.size());
            } catch (Exception e) {
                log.info("批次插入失败：{}", e.getMessage());
                throw e; // 按需处理异常，比如记录日志后重试
            }
        }
    }

    // 工具方法：拆分文档列表为指定大小的批次
    private List<List<Document>> splitDocuments(List<Document> documents, int batchSize) {
        List<List<Document>> batches = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            batches.add(documents.subList(i, end));
        }
        return batches;
    }
}
