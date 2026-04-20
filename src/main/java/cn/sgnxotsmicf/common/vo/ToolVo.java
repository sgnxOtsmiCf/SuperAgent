package cn.sgnxotsmicf.common.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 18:46
 * @Version: 1.0
 * @Description:
 */

@Data
@Builder
public class ToolVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String description;
}
