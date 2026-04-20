package cn.sgnxotsmicf.rag;

import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/24 15:03
 * @Version: 1.0
 * @Description:
 */

public class FilterExpressionBuilderConfig {

    public static FilterExpressionBuilder createFilterExpressionBuilder(){
        return new FilterExpressionBuilder();
    }
}
