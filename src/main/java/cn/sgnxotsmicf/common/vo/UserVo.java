package cn.sgnxotsmicf.common.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/20 17:44
 * @Version: 1.0
 * @Description:
 */

@Data
public class UserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    //private Long id;

    private String username;

    private String nickName;

    private String avatar;

    private String phone;

    private String model;

    private BigDecimal temperature;

    private BigDecimal top_k;

    private BigDecimal top_p;
}
