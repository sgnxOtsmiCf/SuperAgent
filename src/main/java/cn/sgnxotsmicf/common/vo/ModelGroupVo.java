package cn.sgnxotsmicf.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 10:05
 * @Version: 1.0
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String groupName;
    
    private String groupCode;
    
    private Integer sortOrder;

    private List<ModelVo> modelVoList;

}
