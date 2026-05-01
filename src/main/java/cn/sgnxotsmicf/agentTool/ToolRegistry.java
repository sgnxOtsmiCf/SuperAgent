package cn.sgnxotsmicf.agentTool;

import cn.sgnxotsmicf.agentTool.commonTool.*;
import cn.sgnxotsmicf.agentTool.localtool.*;
import cn.sgnxotsmicf.agentTool.onlinetool.*;
import cn.sgnxotsmicf.agentTool.specialTool.TerminateTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/25 15:34
 * @Version: 2.0
 * @Description: Spring AI 工具注册配置类
 */
@Configuration
@RequiredArgsConstructor
public class ToolRegistry {

    // 通用工具
    private final PlanningTool planningTool;
    private final SensitiveFilterTool sensitiveFilterTool;
    // 在线工具
    private final Crawl4aiTool crawl4aiTool;
    private final EmailTool emailTool;
    private final TouTiaoNewsTool touTiaoNewsTool;
    private final WebSearchTool webSearchTool;
    private final TavilySearchTool tavilySearchTool;
    private final SmartWebFetchTool smartWebFetchTool;
    private final OnlineDocumentTool onlineDocumentTool;
    private final DateTimeTool dateTimeTool;
    private final MarkdownToPdfTool markdownToPdfTool;
    private final ImageSearchTool imageSearchTool;
    // 本地工具
    private final BashTool bashTool;
    private final BrowserUseTool browserUseTool;
    private final FileOperationTool fileOperationTool;
    private final PDFGenerationTool pdfGenerationTool;
    private final ResourceDownloadTool resourceDownloadTool;
    private final SandboxTool sandboxTool;
    private final TerminalOperationTool terminalOperationTool;
    // 特殊工具
    private final UserInfoTool userInfoTool;
    private final TerminateTool terminateTool;
    // MCP工具提供者
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    /**
     * 开放工具：通用+在线+MCP+终止工具
     */
    @Bean
    public ToolCallback[] openManusTools() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(getBaseToolList());
        // 添加终止工具
        toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(terminateTool)));
        return toolCallbacks.toArray(ToolCallback[]::new);
    }

    /**
     * 超级代理工具：通用+在线+MCP
     */
    @Bean
    public ToolCallback[] SuperAgentTool() {
        return getBaseToolList().toArray(ToolCallback[]::new);
    }

    /**
     * 基础公共工具：通用+在线+MCP
     */
    private List<ToolCallback> getBaseToolList() {
        List<ToolCallback> baseTools = new ArrayList<>();
        baseTools.addAll(Arrays.asList(commonToolCallbacks()));
        baseTools.addAll(Arrays.asList(onlineToolCallbacks()));
        baseTools.addAll(Arrays.asList(mcpToolCallbacks()));
        return baseTools;
    }

    /**
     * 本地工具
     */
    private ToolCallback[] localToolCallbacks() {
        return ToolCallbacks.from(
                bashTool,
                browserUseTool,
                fileOperationTool,
                pdfGenerationTool,
                resourceDownloadTool,
                sandboxTool,
                terminalOperationTool
        );
    }

    /**
     * 通用工具
     */
    private ToolCallback[] commonToolCallbacks() {
        return ToolCallbacks.from(planningTool, sensitiveFilterTool);
    }

    /**
     * 在线工具
     */
    private ToolCallback[] onlineToolCallbacks() {
        return ToolCallbacks.from(
                crawl4aiTool,
                emailTool,
                touTiaoNewsTool,
                webSearchTool,
                tavilySearchTool,
                smartWebFetchTool,
                onlineDocumentTool,
                dateTimeTool,
                userInfoTool,
                imageSearchTool);
                //markdownToPdfTool);
    }

    /**
     * MCP工具
     */
    private ToolCallback[] mcpToolCallbacks() {
        List<ToolCallback> callbackList = new ArrayList<>(Arrays.asList(toolCallbackProvider.getToolCallbacks()));
        return callbackList.toArray(ToolCallback[]::new);
    }
}