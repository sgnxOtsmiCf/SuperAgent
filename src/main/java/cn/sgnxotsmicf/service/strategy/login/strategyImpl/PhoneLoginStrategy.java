package cn.sgnxotsmicf.service.strategy.login.strategyImpl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import cn.sgnxotsmicf.common.auth.UserInfoContext;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.NickNameGenerator;
import cn.sgnxotsmicf.dao.UserMapper;
import cn.sgnxotsmicf.service.CaptchaService;
import cn.sgnxotsmicf.service.strategy.login.LoginContext;
import cn.sgnxotsmicf.service.strategy.login.LoginStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:46
 * @Version: 0.1
 * @Description:
 */

@Slf4j
@Component
public class PhoneLoginStrategy implements LoginStrategy {

    private static final String PHONE_CODE_KEY_PREFIX = "Login:phone:";

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaService captchaService;

    public PhoneLoginStrategy(UserMapper userMapper,
                              StringRedisTemplate stringRedisTemplate,
                              CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.captchaService = captchaService;
    }

    @Override
    public boolean supports(String loginType) {
        return "phone".equalsIgnoreCase(loginType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SaTokenInfo> login(LoginContext context) {
        String phone = context.getPhone();
        String verifyCode = context.getVerifyCode();
        String captchaId = context.getCaptchaId();
        String captchaCode = context.getCaptchaCode();

        // 1. 校验图片验证码（新增要求）
        captchaService.verifyCaptcha(captchaId, captchaCode);

        // 2. 校验手机号格式
        if (!Validator.isMobile(phone)) {
            return Result.build(ResultCodeEnum.PHONE_FALSE);
        }

        // 3. 查询或创建用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getIsDeleted, 0));

        if (user == null) {
            user = createNewUser(phone);
        }

        // 4. 校验短信验证码
        String storedCode = stringRedisTemplate.opsForValue().get(PHONE_CODE_KEY_PREFIX + phone);
        if (storedCode == null || !storedCode.trim().equals(verifyCode.trim())) {
            return Result.build(ResultCodeEnum.PHONE_CODE_ERROR);
        }
        // 5. 登录并返回
        return Result.ok(UserInfoContext.loginSuffix(user, 24L, TimeUnit.HOURS));
    }

    private User createNewUser(String phone) {
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setNickName(NickNameGenerator.generateChineseNicknameWithSuffix());
        newUser.setUsername("su_" + generateUserName());

        String defaultPassword = "123456";
        String salt = BCrypt.gensalt();
        newUser.setPassword(BCrypt.hashpw(defaultPassword, salt));

        userMapper.insert(newUser);
        return newUser;
    }

    private String generateUserName() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 6);
    }
}

