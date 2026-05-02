package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.UserProfileVo;
import cn.sgnxotsmicf.service.impl.UserProfileServiceImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 21:18
 * @Version: 1.0
 * @Description:
 */

public interface UserProfileService {

    Result<UserProfileVo> getUserProfile();

    Result<UserProfileVo> updateUserProfile(UserProfileServiceImpl.UpdateProfileRequest request);

    Result<UserProfileVo> deleteUserProfile(String dimension, String value);
}
