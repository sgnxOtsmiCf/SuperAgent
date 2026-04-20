package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.dto.CaptchaDTO;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:08
 * @Version: 1.0
 * @Description:
 */

public interface CaptchaService {

    CaptchaDTO generateCaptcha();

    boolean verifyCaptcha(String captchaId, String captchaCode);
}
