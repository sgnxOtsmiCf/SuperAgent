package cn.sgnxotsmicf.database;

import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.dao.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 16:17
 * @Version: 1.0
 * @Description:
 */

@SpringBootTest
public class mysqlTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void contextLoads() {
        User user = new User();
        user.setPassword("123456");
        user.setUsername("admin");
        user.setPhone("13231321421");
        userMapper.insert(user);
    }

}
