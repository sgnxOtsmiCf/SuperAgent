package cn.sgnxotsmicf.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.MinioUtil;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.config.SmsConfig;
import cn.sgnxotsmicf.dao.UserMapper;
import cn.sgnxotsmicf.service.UserService;
import cn.sgnxotsmicf.service.strategy.login.LoginContext;
import cn.sgnxotsmicf.service.strategy.login.LoginStrategyFactory;
import cn.sgnxotsmicf.service.strategy.register.RegisterContext;
import cn.sgnxotsmicf.service.strategy.register.RegisterStrategyFactory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/5 21:05
 * @Version: 1.0
 * @Description:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final MinioUtil minioUtil;

    private final SmsConfig smsConfig;

    private final ServiceUtil serviceUtil;

    private final LoginStrategyFactory loginStrategyFactory;

    private final RegisterStrategyFactory registerStrategyFactory;

    private static final String preKey = "Login:phone:";


    @Override
    public Result<SaTokenInfo> login(LoginContext context) {
        return loginStrategyFactory.getStrategy(context.getLoginType()).login(context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> register(RegisterContext context) {
        return registerStrategyFactory.getStrategy(context.getRegisterType()).register(context);
    }


    /**
     *
     * @param phone 手机号
     * @return 根据手机号进行短信验证服务
     */
    @Override
    public Result<SaTokenInfo> doLoginWithPhoneCodePre(String phone) {
        boolean flag = Validator.isMobile(phone);
        if (!flag) {
            return Result.build(ResultCodeEnum.PHONE_FALSE);
        }
        String verifyCode = smsConfig.sendSmsPhoneCode(phone);
        stringRedisTemplate.opsForValue().set(preKey + phone, verifyCode, 5, TimeUnit.MINUTES);
        return Result.ok();
    }

    @Override
    public Result<String> registerPre(String username) {
        Long count = lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0).count();
        if (count > 0) {
            return Result.build(ResultCodeEnum.ACCOUNT_REPEAT);
        }
        return Result.ok();
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String uploadFileUrl = minioUtil.uploadFile(file);
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        user.setAvatar(uploadFileUrl);
        userMapper.updateById(user);
        return uploadFileUrl;
    }

    @Override
    public String fileSelect() {
        Long userId = serviceUtil.getUserId();
        User user = userMapper.selectById(userId);
        return user.getAvatar();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updatePassword(String password, String newPassword) {
        Long userId = serviceUtil.getUserId();
        User user = getOne(lambdaQuery().eq(User::getId, userId).eq(User::getIsDeleted, 0));
        if (user == null) {
            return Result.build(ResultCodeEnum.PERMISSION);
        }
        String real_password = user.getPassword();
        boolean flag = BCrypt.checkpw(password, real_password);
        if (flag) {
            //旧密码验证成功，相同
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            userMapper.updateById(user);
            return Result.ok();
        }
        return Result.build(ResultCodeEnum.PERMISSION);
    }

    @Override
    public Result<UserVo> getUserInfo() {
        Long userId = serviceUtil.getUserId();
        User user = lambdaQuery()
                .eq(User::getId, userId)
                .eq(User::getIsDeleted, 0)
                .one();

        if (user == null) {
            return Result.build(ResultCodeEnum.PERMISSION);
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return Result.ok(userVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateUserInfo(UserVo userVo) {
        Long realUserId = serviceUtil.getUserId();
        User user = lambdaQuery().eq(User::getId, realUserId).eq(User::getIsDeleted, 0).one();
        if (user == null) {
            return Result.build(ResultCodeEnum.PERMISSION);
        }

        // 如果修改了 username，需要验证是否重复
        if (userVo.getUsername() != null) {
            if (userVo.getUsername().equals(user.getUsername())){
                //这里其实是修改用户名没有变换，但目前这样写也行，逻辑也正确
                return Result.build(ResultCodeEnum.ACCOUNT_REPEAT);
            }else {
                Long count = lambdaQuery()
                        .eq(User::getUsername, userVo.getUsername())
                        .eq(User::getIsDeleted, 0)
                        .count();
                if (count > 0) {
                    return Result.build(ResultCodeEnum.ACCOUNT_REPEAT);
                }
                user.setUsername(userVo.getUsername());
            }

        }

        // 更新 nickName
        if (userVo.getNickName() != null) {
            user.setNickName(userVo.getNickName());
        }

        // 更新 phone
        if (userVo.getPhone() != null) {
            Long count = lambdaQuery().eq(User::getPhone, userVo.getPhone()).eq(User::getIsDeleted, 0).count();
            if (count > 0) {
                return Result.build(ResultCodeEnum.PHONE_REPEAT);
            }
            user.setPhone(userVo.getPhone());
        }

        // 更新到数据库
        userMapper.updateById(user);

        return Result.ok();
    }

}
