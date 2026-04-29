package cn.sgnxotsmicf.common.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.exception.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 14:06
 * @Version: 1.0
 * @Description:
 */

@Slf4j
public class UserInfoContext {


    /**
     * 登录后置处理：执行登录并缓存用户信息到 Sa-Session
     *
     * @param user     数据库查询到的用户实体
     * @param timeout  Token 有效期（传入的参数，实际使用 unit 转换）
     * @param unit     时间单位,为null时为秒(s)
     * @return SaTokenInfo Token 信息
     */
    public static SaTokenInfo loginSuffix(User user, Long timeout, TimeUnit unit) {
        if (user == null || user.getId() == null) {
            throw new AgentException(ResultCodeEnum.PERMISSION);
        }
        long timeoutSeconds = unit != null ? unit.toSeconds(timeout) : 60 * 60 * 24;
        StpUtil.login(user.getId(), timeoutSeconds);
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        StpUtil.getSession().set("userInfo", userVo);
        return StpUtil.getTokenInfo();
    }


    /**
     * 获取当前登录用户信息
     */
    public static UserVo getCurrentUser() {
        SaSession session = StpUtil.getSession();
        UserVo userVo = (UserVo) session.get("userInfo");
        if (userVo == null) {
            // Session 中无用户信息，可能已过期或被清理
            log.error("Session 中无用户信息，可能已过期或被清理");
            throw new AgentException(ResultCodeEnum.PERMISSION);
        }
        return userVo;
    }


    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }


    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        UserVo userVo = getCurrentUser();
        return userVo.getUsername();
    }

    /**
     * 更新当前 Session 中的用户信息（如用户修改资料后）
     */
    public static void refreshUserInfo(User user) {
        StpUtil.checkLogin();
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        StpUtil.getSession().set("userInfo", userVo);
    }

}
