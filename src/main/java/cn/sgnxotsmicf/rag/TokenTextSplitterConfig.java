package cn.sgnxotsmicf.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 14:24
 * @Version: 1.0
 * @Description:
 */

public class TokenTextSplitterConfig {

    public static List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

}
