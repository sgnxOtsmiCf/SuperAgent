package cn.sgnxotsmicf;

import cn.sgnxotsmicf.agentTool.onlinetool.SmartWebFetchTool;
import cn.sgnxotsmicf.app.superagent.SuperAgentFactory;
import cn.sgnxotsmicf.common.tools.AgentCommon;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.tokenizers.QwenTokenizer;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/7 20:42
 * @Version: 1.0
 * @Description:
 */
@SpringBootTest
public class test {

    @Resource
    private SuperAgentFactory superAgentFactory;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void token2(){
        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        //Set<String> sessionList = stringRedisTemplate.opsForSet().members(AgentCommon.RedisAgentSessionKeyPrefix + 2);
        //assert sessionList != null;
        List<String> sessionList = new ArrayList<>();
        sessionList.add("session_1775807850496_nc0nzmqa3");
        sessionList.add("session_1775809696459_q5zmvmj2i");
        sessionList.add("session_1775811440947_vq5ywwcpr");
        sessionList.forEach(sessionId -> {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(sessionId).build();
            Collection<Checkpoint> checkpoints = redisSaver.list(runnableConfig);
            System.out.println("=================={"+sessionId+"}========================");
            for (Checkpoint checkpoint : checkpoints) {
                Map<String, Object> checkpointState = checkpoint.getState();
                System.out.println("===========================start================================");
                System.out.println(checkpointState);
                System.out.println("===========================end=====================================");
            }
        });
//        String string = stringRedisTemplate.opsForValue().get("graph:checkpoint:content:f456c6d2-fa50-49cb-9784-a37534c8e186");
//        System.out.println(string);
    }

    @Test
    public void test(){
        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        //Set<String> sessionList = stringRedisTemplate.opsForSet().members(AgentCommon.RedisAgentSessionKeyPrefix + 2);
        ArrayList<String> sessionList = new ArrayList<>();
        sessionList.add("5bab5a4a-d88f-4858-a44b-a0b898d27e76");
        assert sessionList != null;
        sessionList.forEach(sessionId -> {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(sessionId).build();
            Optional<Checkpoint> optionalCheckpoint = redisSaver.get(runnableConfig);
            optionalCheckpoint.ifPresent(checkpoint -> {
                System.out.println("=========================start===============================");
                System.out.println(checkpoint);
                ArrayList<Message> list = (ArrayList) checkpoint.getState().get("messages");

                //id state nodeId nextNodeId
                //state: {media-[] messageType textContent metadata}
                System.out.println(checkpoint);
                System.out.println("==========================end======================================");
            });
        });
    }

    @Resource
    private ChatModel dashScopeChatModel;

    @Resource
    private ChatModel openAiChatModel;

    @Resource
    private ChatModel zhiPuAiChatModel;


    @Test
    public void test2(){
        System.out.println("=========================start===============================");
        String call1 = dashScopeChatModel.call("你好,你是哪一款模型,1+1=?");
        System.out.println(call1);
        System.out.println("=========================end===============================");
//        System.out.println("=========================start===============================");
//        String call2 = openAiChatModel.call("你好,你是哪一款模型,1+1=?");
//        System.out.println(call2);
//        System.out.println("=========================end===============================");
//        System.out.println("=========================start===============================");
//        String call3 = zhiPuAiChatModel.call("你好,你是哪一款模型");
//        System.out.println(call3);
//        System.out.println("=========================end===============================");
    }

    @Test
    public void test3() throws GraphRunnerException {
        ReactAgent reactAgent = superAgentFactory.createAgent(Map.of(),SuperAgentFactory.MODEL_QWEN_PLUS);
        System.out.println(reactAgent.call("你好,你是哪一款模型",RunnableConfig.builder().threadId("1").build()).getText());
    }
}