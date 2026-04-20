package cn.sgnxotsmicf.advisor;

import jakarta.annotation.Resource;
import org.springaicommunity.tool.search.ToolSearchToolCallAdvisor;
import org.springaicommunity.tool.search.ToolSearcher;
import org.springaicommunity.tool.searcher.LuceneToolSearcher;
import org.springaicommunity.tool.searcher.RegexToolSearcher;
import org.springaicommunity.tool.searcher.VectorToolSearcher;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 22:40
 * @Version: 1.0
 * @Description: 暂时spring alibaba 不兼容
 */
@Component
public class SearchToolAdvisor {

    @Resource(name = "pgVectorStore")
    private VectorStore vectorStore;


    @Bean
    ToolSearcher toolSearcherLucene() {
        return new LuceneToolSearcher(0.4f);
    }

    @Bean
    ToolSearcher toolSearcherVector(){
        return new VectorToolSearcher(vectorStore);
    }

    @Bean
    ToolSearcher toolSearcherRegex(){
        return new RegexToolSearcher();
    }


    @Bean
    public Advisor ToolSearchToolCallAdvisor(ToolSearcher toolSearcherLucene) {
        return ToolSearchToolCallAdvisor.builder()
                    .toolSearcher(toolSearcherLucene)
                    .build();

    }
}
