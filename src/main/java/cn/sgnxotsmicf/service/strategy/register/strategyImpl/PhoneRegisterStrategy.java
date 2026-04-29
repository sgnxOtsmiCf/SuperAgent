package cn.sgnxotsmicf.service.strategy.register.strategyImpl;

import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import cn.sgnxotsmicf.common.auth.RoleConstant;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.po.UserRole;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.NickNameGenerator;
import cn.sgnxotsmicf.dao.UserMapper;
import cn.sgnxotsmicf.dao.UserRoleMapper;
import cn.sgnxotsmicf.service.CaptchaService;
import cn.sgnxotsmicf.service.strategy.register.RegisterContext;
import cn.sgnxotsmicf.service.strategy.register.RegisterStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:50
 * @Version: 0.1
 * @Description: 手机号登录策略
 */

@Slf4j
@Component
public class PhoneRegisterStrategy implements RegisterStrategy {

    private static final String PHONE_CODE_KEY_PREFIX = "Login:phone:";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaService captchaService;

    public PhoneRegisterStrategy(UserMapper userMapper,
                                 UserRoleMapper userRoleMapper,
                                 RedissonClient redissonClient,
                                 StringRedisTemplate stringRedisTemplate,
                                 CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.captchaService = captchaService;
    }

    @Override
    public boolean supports(String registerType) {
        return "phone".equalsIgnoreCase(registerType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> register(RegisterContext context) {
        String phone = context.getPhone();
        String verifyCode = context.getVerifyCode();

        // 1. 校验图片验证码
        captchaService.verifyCaptcha(context.getCaptchaId(), context.getCaptchaCode());

        // 2. 校验手机号格式
        if (!Validator.isMobile(phone)) {
            return Result.build(ResultCodeEnum.PHONE_FALSE);
        }

        // 3. 校验短信验证码
        String storedCode = stringRedisTemplate.opsForValue().get(PHONE_CODE_KEY_PREFIX + phone);
        if (storedCode == null || !storedCode.trim().equals(verifyCode.trim())) {
            return Result.build(ResultCodeEnum.PHONE_CODE_ERROR);
        }

        RLock lock = redissonClient.getLock("lock:register:phone:" + phone);
        boolean isLock = lock.tryLock();
        try {
            if (isLock) {
                // 4. 检查手机号是否已注册
                Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .eq(User::getIsDeleted, 0));
                if (count > 0) {
                    return Result.build(ResultCodeEnum.PHONE_REPEAT);
                }

                // 5. 创建新用户
                User user = new User();
                user.setPhone(phone);
                user.setUsername("su_" + generateUserName());
                String defaultPassword = "123456";
                String salt = BCrypt.gensalt();
                user.setPassword(BCrypt.hashpw(defaultPassword, salt));
                user.setNickName(NickNameGenerator.generateChineseNicknameWithSuffix());
                userMapper.insert(user);

                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(RoleConstant.USER_ID);
                userRoleMapper.insert(userRole);

                return Result.ok();
            }
            return Result.build(ResultCodeEnum.SERVICE_ERROR);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }

    private String generateUserName() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 6);
    }
}

