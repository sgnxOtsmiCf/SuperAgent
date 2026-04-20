package cn.sgnxotsmicf.app.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayDeque;
import java.util.Deque;

@SpringBootTest
class ManusTest {



    @Test
    void run2() throws Exception {
        Deque<Character> stack = new ArrayDeque<>();
        stack.push('a');
        stack.push('b');
        Character peek = stack.peek();
        System.out.println(peek);
    }
}