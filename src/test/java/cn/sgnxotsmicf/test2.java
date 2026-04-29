package cn.sgnxotsmicf;

import cn.sgnxotsmicf.app.superagent.factory.SuperAgentFactory;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.service.ChatSessionService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/13 19:55
 * @Version: 1.0
 * @Description:
 */
@SpringBootTest
public class test2 {

    @Autowired
    private ServiceUtil serviceUtil;

    @Autowired
    private SuperAgentFactory superAgentFactory;

    @Autowired
    private ChatSessionService chatSessionService;

    @Test
    public void test() {
        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        ArrayList<String> sessionIdList = new ArrayList<>();
        sessionIdList.add("SA00TDQDFDqQMbxr18mg");
        List<ChatSessionVo> sessionVoList = new ArrayList<>();
        for (String sessionId : sessionIdList) {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(sessionId).build();
            Optional<Checkpoint> optionalCheckpoint = redisSaver.get(runnableConfig);
            optionalCheckpoint.ifPresent(checkpoint -> {
                ArrayList<Message> list = (ArrayList) checkpoint.getState().get("messages");
                System.out.println("Message:"+list);
                //id state nodeId nextNodeId
                //state.messages: {media-[] messageType textContent metadata}
            });
        }
        System.out.println(sessionVoList);
    }

}
