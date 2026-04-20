package cn.sgnxotsmicf.advisor;

import cn.sgnxotsmicf.chatMemory.NoSqlChatMemoryFactory;
import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.model.RerankModel;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/15 13:55
 * @Version: 1.0
 * @Description:
 */

@Component
public class AdvisorRegister {


    @Resource(name = "pgVectorStore")
    private VectorStore pgVectorStore;

    @Resource
    private RerankModel dashscopeRerankModel;

    @Resource
    private AgentLogAdvisor agentLogAdvisor;

    @Resource
    private NoSqlChatMemoryFactory noSqlChatMemoryFactory;

    public List<Advisor> buildAdvisors(int maxRounds) {
        ArrayList<Advisor> advisors = new ArrayList<Advisor>();
        advisors.add(retrievalRerankAdvisor());
        advisors.add(reActProtocolAdvisor(maxRounds));
        //advisors.add(agentLogAdvisor);
        return advisors;
    }

    /**
     * 重排模型
     * @return Advisor对象
     */
    public Advisor retrievalRerankAdvisor(){
        return new RetrievalRerankAdvisor(pgVectorStore, dashscopeRerankModel, 0.75);
    }

    /**
     * ReAct校准
     * @param maxRounds 最大循环次数
     * @return Advisor对象
     */
    public Advisor reActProtocolAdvisor(int maxRounds){
        return new ReActProtocolAdvisor(maxRounds);
    }

    /**
     * 工具过滤器------现在真没有理解，禁用
     * @return Advisor对象
     */
    public Advisor toolCallAdvisor(){
        DefaultToolCallingManager defaultToolCallingManager = DefaultToolCallingManager.builder().build();
        // Disable internal history - let ChatMemory handle it
        return ToolCallAdvisor.builder()
                .toolCallingManager(defaultToolCallingManager)
                .disableMemory()  // Disable internal history - let ChatMemory handle it
                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                .build();
    }
}
