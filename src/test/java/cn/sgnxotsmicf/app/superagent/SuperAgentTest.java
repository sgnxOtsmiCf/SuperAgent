package cn.sgnxotsmicf.app.superagent;

import cn.hutool.core.util.IdUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SuperAgentTest {

    @Resource
    private SuperAgent superAgent;

    @Test
    void doChatAgent() {

    }
}