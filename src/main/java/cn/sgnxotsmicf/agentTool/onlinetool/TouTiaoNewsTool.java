package cn.sgnxotsmicf.agentTool.onlinetool;

import com.alibaba.cloud.ai.toolcalling.toutiaonews.ToutiaoNewsSearchHotEventsService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/27 15:40
 * @Version: 1.0
 * @Description:
 */
@Component
public class TouTiaoNewsTool {

    @Resource
    private ToutiaoNewsSearchHotEventsService getToutiaoNews;

    @Tool(description = "Get the latest hot news from Toutiao(获取今日头条最新热点新闻)")
    public String getTouTiaoNews(){
        return getToutiaoNews.apply(new ToutiaoNewsSearchHotEventsService.Request()).events().toString();
    }
}
