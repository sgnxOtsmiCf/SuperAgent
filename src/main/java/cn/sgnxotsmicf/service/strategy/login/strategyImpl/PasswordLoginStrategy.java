package cn.sgnxotsmicf.service.strategy.login.strategyImpl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.sgnxotsmicf.common.auth.UserInfoContext;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.dao.UserMapper;
import cn.sgnxotsmicf.service.CaptchaService;
import cn.sgnxotsmicf.service.strategy.login.LoginContext;
import cn.sgnxotsmicf.service.strategy.login.LoginStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:46
 * @Version: 0.1
 * @Description:
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final UserMapper userMapper;

    private final CaptchaService captchaService;


    @Override
    public boolean supports(String loginType) {
        return "password".equalsIgnoreCase(loginType);
    }

    @Override
    public Result<SaTokenInfo> login(LoginContext context) {
        // 校验图片验证码
        captchaService.verifyCaptcha(context.getCaptchaId(), context.getCaptchaCode());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, context.getUsername())
                .eq(User::getIsDeleted, 0));

        if (user == null) {
            return Result.build(ResultCodeEnum.ACCOUNT_ERROR);
        }

        if (!BCrypt.checkpw(context.getPassword(), user.getPassword())) {
            return Result.build(ResultCodeEnum.PASSWORD_ERROR);
        }

        return Result.ok(UserInfoContext.loginSuffix(user, 24L, TimeUnit.HOURS));
    }
}

