package cn.sgnxotsmicf.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/20 17:30
 * @Version: 1.0
 * @Description:
 */

@Component
@Slf4j
public class DocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public DocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkDowns(String Path) {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(Path);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                assert fileName != null;
                MarkdownDocumentReaderConfig markdownDocumentReaderConfig = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, markdownDocumentReaderConfig);
                documents.addAll(markdownDocumentReader.read());
            }
        }catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return documents;
    }

}
