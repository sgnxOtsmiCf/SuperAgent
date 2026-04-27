package cn.sgnxotsmicf.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import cn.sgnxotsmicf.common.auth.RoleConstant;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.po.UserRole;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.MinioUtil;
import cn.sgnxotsmicf.common.tools.NickNameGenerator;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.config.SmsConfig;
import cn.sgnxotsmicf.dao.UserMapper;
import cn.sgnxotsmicf.dao.UserRoleMapper;
import cn.sgnxotsmicf.service.CaptchaService;
import cn.sgnxotsmicf.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/5 21:05
 * @Version: 1.0
 * @Description:
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleMapper userRoleMapper;

    private final UserMapper userMapper;

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final MinioUtil minioUtil;

    private final SmsConfig smsConfig;

    private final CaptchaService captchaService;

    private final ServiceUtil serviceUtil;

    private static final String preKey = "Login:phone:";

    public UserServiceImpl(UserRoleMapper userRoleMapper, UserMapper userMapper, RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate, MinioUtil minioUtil, SmsConfig smsConfig, CaptchaService captchaService, ServiceUtil serviceUtil) {
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.minioUtil = minioUtil;
        this.smsConfig = smsConfig;
        this.captchaService = captchaService;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Result<SaTokenInfo> doLogin(String username, String password, String captchaId, String captchaCode) {
        captchaService.verifyCaptcha(captchaId, captchaCode);
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0));
        if (user == null) {
            return Result.build(ResultCodeEnum.ACCOUNT_ERROR);
        }
        if (BCrypt.checkpw(password, user.getPassword())) {
            StpUtil.login(user.getId(),60 * 60 * 24);
            return Result.ok(StpUtil.getTokenInfo());
        }
        return Result.build(ResultCodeEnum.PASSWORD_ERROR);
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

    /**
     *
     * @param phone 手机号
     * @param verifyCode 短信验证码
     * @return 根据手机号和短信验证码进行登录|注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SaTokenInfo> doLoginWithPhoneCode(String phone, String verifyCode) {
        boolean flag = Validator.isMobile(phone);
        if (!flag) {
            return Result.build(ResultCodeEnum.PHONE_FALSE);
        }
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getIsDeleted, 0));
        if (user == null) {
            //用户不存在，创建新用户
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setNickName(NickNameGenerator.generateChineseNicknameWithSuffix());
            newUser.setUsername("su_" + generateUserName());
            String password = "123456"; //默认密码
            String salt = BCrypt.gensalt();
            String BCryptPassword = BCrypt.hashpw(password, salt);
            newUser.setPassword(BCryptPassword);
            userMapper.insert(newUser);
        }
        String storeCode = stringRedisTemplate.opsForValue().get(preKey + phone);
        assert storeCode != null;
        if (!storeCode.trim().equals(verifyCode.trim())) {
            return Result.build(ResultCodeEnum.PHONE_CODE_ERROR);
        }
        assert user != null;
        StpUtil.login(user.getId(),60 * 60 * 24);
        return Result.ok(StpUtil.getTokenInfo());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> register(String username, String password) {
        RLock lock = redissonClient.getLock("lock:register:" + username);
        boolean isLock = lock.tryLock();
        try {
            if (isLock) {
                if (Objects.equals(registerPre(username).getCode(), ResultCodeEnum.ACCOUNT_REPEAT.getCode())) {
                    return Result.build(ResultCodeEnum.ACCOUNT_REPEAT);
                }
                User user = new User();
                user.setUsername(username);
                String salt = BCrypt.gensalt();
                String BCryptPassword = BCrypt.hashpw(password, salt);
                user.setPassword(BCryptPassword);
                user.setNickName(NickNameGenerator.generateChineseNicknameWithSuffix());
                save(user);
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(RoleConstant.USER_ID);
                userRoleMapper.insert(userRole);
                return Result.ok();
            }
            return Result.build(ResultCodeEnum.SERVICE_ERROR);
        } catch (Exception e) {
            log.error(e.getMessage());
        }finally {
            lock.unlock();
        }
        return null;
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
    public String fileSelect(Long userId) {
        User user = userMapper.selectById(userId);
        return user.getAvatar();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updatePassword(Long userId, String password, String newPassword) {
        Long realUserId = serviceUtil.getUserId();
        if (!Objects.equals(realUserId, userId)) {
            return Result.build(ResultCodeEnum.PERMISSION);
        }
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
    public Result<UserVo> getUserInfo(Long userId) {
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

        // 更新 model
        if (userVo.getModel() != null) {
            user.setModel(userVo.getModel());
        }

        // 更新 temperature
        if (userVo.getTemperature() != null) {
            user.setTemperature(userVo.getTemperature());
        }

        // 更新 top_k
        if (userVo.getTopK() != null) {
            user.setTopK(userVo.getTopK());
        }

        // 更新 top_p
        if (userVo.getTopP() != null) {
            user.setTopP(userVo.getTopP());
        }

        // 更新到数据库
        userMapper.updateById(user);

        return Result.ok();
    }


    private String generateUserName() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = uuid.substring(0, 6);
        return "su_" + suffix;
    }
}
