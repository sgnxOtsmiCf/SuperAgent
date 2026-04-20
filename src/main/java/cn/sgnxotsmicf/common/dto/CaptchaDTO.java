package cn.sgnxotsmicf.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;


/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:01
 * @Version: 1.0
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String captchaId;

    private String captchaImage;
}
