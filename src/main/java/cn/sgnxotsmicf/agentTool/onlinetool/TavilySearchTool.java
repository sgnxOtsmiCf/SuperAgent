package cn.sgnxotsmicf.agentTool.onlinetool;

import cn.sgnxotsmicf.agentTool.config.TavilySearchProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Tavily AI 专业搜索工具
 * 全覆盖 Tavily 所有API功能，符合Spring AI工具规范
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilySearchTool {

    private final TavilySearchProperties properties;
    private final WebClient.Builder webClientBuilder;

    /**
     * Tavily AI智能搜索（支持新闻、网页、深度搜索、图片、过滤等全功能）
     * @param query 搜索关键词/问题（必填）
     * @param searchDepth 搜索深度: basic(快速) / advanced(深度，消耗更多额度)
     * @param topic 搜索主题: general(通用) / news(新闻)
     * @param maxResults 最大返回结果数(1-10，默认5)
     * @param includeAnswer 是否返回AI总结答案(默认true)
     * @param includeRawContent 是否返回网页原始内容(默认false)
     * @param includeImages 是否返回相关图片链接(默认false)
     * @param includeSummary 是否返回结果摘要(默认true)
     * @param timeRange 时间范围: day(一天) / week(一周) / month(一月) / year(一年)
     * @param includeDomains 仅搜索指定域名(例: ["baidu.com","github.com"])
     * @param excludeDomains 排除指定域名(例: ["qq.com"])
     */
    @Tool(
            name = "tavily_search",
            description = "Professional AI search engine supporting general search, news search, deep search, image search, domain filtering and time range filtering for accurate online information query"
    )
    public String tavilySearch(
            @ToolParam(description = "Search query keywords or user questions", required = true)
            String query,

            @ToolParam(description = "Search depth mode: basic(fast) or advanced(deep, more credits)", required = false)
            String searchDepth,

            @ToolParam(description = "Search topic type: general(common search) or news(news only)", required = false)
            String topic,

            @ToolParam(description = "Maximum number of search results, range 1-10, default 5", required = false)
            Integer maxResults,

            @ToolParam(description = "Whether to include AI-generated summary answer", required = false)
            Boolean includeAnswer,

            @ToolParam(description = "Whether to include raw full content of web pages", required = false)
            Boolean includeRawContent,

            @ToolParam(description = "Whether to include related image URLs from search results", required = false)
            Boolean includeImages,

            @ToolParam(description = "Whether to include brief summary for each search result", required = false)
            Boolean includeSummary,

            @ToolParam(description = "Time range limit: day, week, month, year", required = false)
            String timeRange,

            @ToolParam(description = "List of domains allowed to search, e.g. [\"github.com\"]", required = false)
            List<String> includeDomains,

            @ToolParam(description = "List of domains excluded from search, e.g. [\"example.com\"]", required = false)
            List<String> excludeDomains
    ) {
        try {
            WebClient webClient = webClientBuilder.build();

            TavilyRequest request = new TavilyRequest();
            request.setApiKey(properties.getApiKey());
            request.setQuery(query);

            // 默认值处理
            request.setSearchDepth(searchDepth == null ? "basic" : searchDepth);
            request.setTopic(topic == null ? "general" : topic);
            request.setMaxResults(maxResults == null ? 5 : Math.min(maxResults, 10));
            request.setIncludeAnswer(includeAnswer == null || includeAnswer);
            request.setIncludeRawContent(includeRawContent != null && includeRawContent);
            request.setIncludeImages(includeImages != null && includeImages);
            request.setIncludeSummary(includeSummary == null || includeSummary);
            request.setTimeRange(timeRange);
            request.setIncludeDomains(includeDomains);
            request.setExcludeDomains(excludeDomains);

            TavilyResponse response = webClient.post()
                    .uri(properties.getApiUrl())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TavilyResponse.class)
                    .retry(properties.getMaxRetries())
                    .block();

            return formatSearchResult(response);

        } catch (Exception e) {
            log.error("Tavily搜索失败", e);
            return "Tavily search exception: " + e.getMessage();
        }
    }

    // ==================== 结果格式化 ====================
    private String formatSearchResult(TavilyResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return "No search results available";
        }

        StringBuilder sb = new StringBuilder();

        // AI 总结答案
        if (response.getAnswer() != null && !response.getAnswer().isEmpty()) {
            sb.append("【AI Summary Answer】：").append(response.getAnswer()).append("\n\n");
        }

        // 搜索结果
        sb.append("【Search Results】：\n");
        for (TavilyResult result : response.getResults()) {
            sb.append("Title：").append(result.getTitle()).append("\n");
            sb.append("Url：").append(result.getUrl()).append("\n");

            // 优先使用 content，如果没有则使用 summary
            String content = result.getContent() != null ? result.getContent() : result.getSummary();
            if (content != null) {
                sb.append("Content：").append(content).append("\n");
            }

            if (result.getScore() != null) {
                sb.append("Score：").append(String.format("%.4f", result.getScore())).append("\n");
            }

            // 原始内容（如果请求了）
            if (result.getRawContent() != null) {
                sb.append("Raw Content：")
                        .append(result.getRawContent(), 0, Math.min(500, result.getRawContent().length()))
                        .append("...\n");
            }

            // 结果中的图片
            if (result.getImages() != null && !result.getImages().isEmpty()) {
                sb.append("Images in result：\n");
                for (String imgUrl : result.getImages()) {
                    sb.append("  - ").append(imgUrl).append("\n");
                }
            }

            sb.append("----------------------------------------\n");
        }

        // 全局图片列表
        if (response.getImages() != null && !response.getImages().isEmpty()) {
            sb.append("【Related Images】：\n");
            for (String imgUrl : response.getImages()) {
                sb.append("  - ").append(imgUrl).append("\n");
            }
        }

        // 后续问题建议
        if (response.getFollowUpQuestions() != null && !response.getFollowUpQuestions().isEmpty()) {
            sb.append("\n【Follow-up Questions】：\n");
            for (String question : response.getFollowUpQuestions()) {
                sb.append("  ? ").append(question).append("\n");
            }
        }

        // 响应时间和用量信息
        if (response.getResponseTime() != null) {
            sb.append("\nResponse Time：").append(response.getResponseTime()).append("s");
        }
        if (response.getUsage() != null && response.getUsage().getCredits() != null) {
            sb.append(" | Credits Used：").append(response.getUsage().getCredits());
        }
        if (response.getRequestId() != null) {
            sb.append(" | Request ID：").append(response.getRequestId());
        }

        return sb.toString();
    }

    // ==================== Tavily 请求/响应实体 ====================

    @Data
    private static class TavilyRequest {
        @JsonProperty("api_key")
        private String apiKey;
        @JsonProperty("query")
        private String query;
        @JsonProperty("search_depth")
        private String searchDepth;
        @JsonProperty("topic")
        private String topic;
        @JsonProperty("max_results")
        private Integer maxResults;
        @JsonProperty("include_answer")
        private Boolean includeAnswer;
        @JsonProperty("include_raw_content")
        private Boolean includeRawContent;
        @JsonProperty("include_images")
        private Boolean includeImages;
        @JsonProperty("include_summary")
        private Boolean includeSummary;
        @JsonProperty("time_range")
        private String timeRange;
        @JsonProperty("include_domains")
        private List<String> includeDomains;
        @JsonProperty("exclude_domains")
        private List<String> excludeDomains;
    }

    @Data
    // 忽略未知字段，防止API未来新增字段导致报错
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TavilyResponse {
        @JsonProperty("query")
        private String query;
        @JsonProperty("answer")
        private String answer;
        @JsonProperty("results")
        private List<TavilyResult> results;
        @JsonProperty("images")
        private List<String> images;
        @JsonProperty("response_time")
        private String responseTime;  // 官方文档显示是字符串 "1.67"
        @JsonProperty("follow_up_questions")
        private List<String> followUpQuestions;
        @JsonProperty("request_id")
        private String requestId;  // ✅ 新增：请求ID
        @JsonProperty("auto_parameters")
        private AutoParameters autoParameters;  // ✅ 新增：自动参数
        @JsonProperty("usage")
        private UsageInfo usage;  // ✅ 新增：用量信息
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TavilyResult {
        @JsonProperty("title")
        private String title;
        @JsonProperty("url")
        private String url;
        @JsonProperty("content")
        private String content;  // ✅ 新增：主要内容（官方文档中的主要字段）
        @JsonProperty("summary")
        private String summary;  // 保留，但 content 是主要字段
        @JsonProperty("score")
        private Double score;
        @JsonProperty("raw_content")
        private String rawContent;
        @JsonProperty("favicon")
        private String favicon;  // ✅ 新增：网站图标
        @JsonProperty("images")
        private List<String> images;  // ✅ 修正：结果中的图片是对象列表，不是字符串列表
    }

    // ✅ 新增：图片信息对象
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ImageInfo {
        @JsonProperty("url")
        private String url;
        @JsonProperty("description")
        private String description;
    }

    // ✅ 新增：自动参数对象
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AutoParameters {
        @JsonProperty("topic")
        private String topic;
        @JsonProperty("search_depth")
        private String searchDepth;
    }

    // ✅ 新增：用量信息对象
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UsageInfo {
        @JsonProperty("credits")
        private Integer credits;
    }
}