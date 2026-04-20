package cn.sgnxotsmicf.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.sgnxotsmicf.common.po.User;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.UserVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/5 21:05
 * @Version: 1.0
 * @Description:
 */
@Mapper
public interface UserService extends IService<User> {
    /*登录*/
    Result<SaTokenInfo> doLogin(String username, String password, String captchaId, String captchaCode);

    Result<SaTokenInfo> doLoginWithPhoneCodePre(String phone);

    Result<SaTokenInfo> doLoginWithPhoneCode(String phone, String verifyCode);
    /*注册*/
    Result<String> register(String username, String password);

    Result<String> registerPre(String username);

    String uploadFile(MultipartFile file);

    String fileSelect(Long userId);

    Result<String> updatePassword(Long userId, String password, String newPassword);

    Result<UserVo> getUserInfo(Long userId);

    Result<String> updateUserInfo(UserVo userVo);
}
