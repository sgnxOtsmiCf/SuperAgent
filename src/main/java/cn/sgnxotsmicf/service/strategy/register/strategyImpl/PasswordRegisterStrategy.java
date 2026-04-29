package cn.sgnxotsmicf.service.strategy.register.strategyImpl;

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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:49
 * @Version: 0.1
 * @Description: 账号密码登录策略
 */

@Slf4j
@Component
public class PasswordRegisterStrategy implements RegisterStrategy {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RedissonClient redissonClient;
    private final CaptchaService captchaService;

    public PasswordRegisterStrategy(UserMapper userMapper,
                                    UserRoleMapper userRoleMapper,
                                    RedissonClient redissonClient,
                                    CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.redissonClient = redissonClient;
        this.captchaService = captchaService;
    }

    @Override
    public boolean supports(String registerType) {
        return "password".equalsIgnoreCase(registerType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> register(RegisterContext context) {
        String username = context.getUsername();
        String password = context.getPassword();

        // 校验图片验证码
        captchaService.verifyCaptcha(context.getCaptchaId(), context.getCaptchaCode());

        RLock lock = redissonClient.getLock("lock:register:" + username);
        boolean isLock = lock.tryLock();
        try {
            if (isLock) {
                // 检查用户名是否已存在
                Long count = userMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.sgnxotsmicf.common.po.User>()
                                .eq(cn.sgnxotsmicf.common.po.User::getUsername, username)
                                .eq(cn.sgnxotsmicf.common.po.User::getIsDeleted, 0));
                if (count > 0) {
                    return Result.build(ResultCodeEnum.ACCOUNT_REPEAT);
                }

                User user = new User();
                user.setUsername(username);
                String salt = BCrypt.gensalt();
                user.setPassword(BCrypt.hashpw(password, salt));
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
}

