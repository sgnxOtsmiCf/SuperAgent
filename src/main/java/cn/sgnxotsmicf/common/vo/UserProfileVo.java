package cn.sgnxotsmicf.common.vo;
import lombok.Data;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/20 14:15
 * @Version: 1.0
 * @Description: 用户画像
 */

@Data
public class UserProfileVo {
    private Long userId;
    private List<ProfileItemVo> profiles;
}

