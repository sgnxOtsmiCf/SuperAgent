package cn.sgnxotsmicf.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.sgnxotsmicf.common.dto.CaptchaDTO;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:08
 * @Version: 1.0
 * @Description:
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final RedissonClient redissonClient;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:login:";

    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    @Override
    public CaptchaDTO generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 5);
        String captchaId = IdUtil.fastSimpleUUID();
        String captchaCode = captcha.getCode();

        RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY_PREFIX + captchaId);
        bucket.set(captchaCode.toLowerCase(), CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        String base64Image = captcha.getImageBase64Data();

        CaptchaDTO dto = new CaptchaDTO();
        dto.setCaptchaId(captchaId);
        dto.setCaptchaImage(base64Image);
        return dto;
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new AgentException(ResultCodeEnum.CAPTCHA_EMPTY);
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String storedCode = bucket.get();

        if (storedCode == null) {
            throw new AgentException(ResultCodeEnum.CAPTCHA_EXPIRED);
        }

        boolean match = storedCode.equalsIgnoreCase(captchaCode.trim());
        if (match) {
            bucket.delete();
        } else {
            throw new AgentException(ResultCodeEnum.CAPTCHA_ERROR);
        }
        return true;
    }
}
