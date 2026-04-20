package cn.sgnxotsmicf.agentTool.localtool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.sgnxotsmicf.agentTool.config.ToolLocalConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
@Component
public class ResourceDownloadTool {

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url, @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {
        String fileDir = ToolLocalConfig.FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 使用 Hutool 的 downloadFile 方法下载资源
            HttpResponse response = HttpRequest.get(url)
                    // 模拟浏览器User-Agent
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36 Edg/146.0.0.0")
                    .timeout(30000)
                    .execute();
            if (response.isOk()){
                response.writeBody(new File(filePath));
            }else {
                return "Error downloading resource，status code：" + response.getStatus() +
                        ",response content:" + response.body();
            }
            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
