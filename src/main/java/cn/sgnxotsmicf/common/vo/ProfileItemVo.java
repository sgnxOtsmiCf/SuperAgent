package cn.sgnxotsmicf.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/20 14:15
 * @Version: 1.0
 * @Description: 单个画像维度条目
 */

@Data
public class ProfileItemVo {
    /**
     * 维度名称 (如 "偏好颜色"、"姓名"、"技术栈")
     */
    private String key;

    /**
     * 维度具体内容
     */
    private String value;

    /**
     * 更新时间戳
     */
    private Long updatedAt;

    /**
     * 格式化后的更新时间，方便前端直接展示
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String updatedTimeStr;
}