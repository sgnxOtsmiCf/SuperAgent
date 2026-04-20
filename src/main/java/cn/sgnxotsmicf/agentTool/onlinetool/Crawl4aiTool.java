package cn.sgnxotsmicf.agentTool.onlinetool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Crawl4aiTool {

    // 完全保留你原始的英文描述
    @Tool(description = "Web crawler that extracts clean, AI-ready content from web pages. Features: Extracts clean markdown content optimized for LLMs, supports multiple URLs in a single request, validates URLs automatically, filters content by word count threshold, fast and reliable with built-in error handling.")
    public CrawlResult crawl(
            @ToolParam(description = "List of URLs to crawl. Can be a single URL or multiple URLs.") List<String> urls,
            @ToolParam(description = "Timeout in seconds for each URL. Default is 30.") Integer timeout,
            @ToolParam(description = "Whether to bypass cache and fetch fresh content. Default is false.") Boolean bypassCache,
            @ToolParam(description = "Minimum word count for content blocks. Default is 10.") Integer wordCountThreshold
    ) {
        if (timeout == null) timeout = 30;
        if (bypassCache == null) bypassCache = false;
        if (wordCountThreshold == null) wordCountThreshold = 10;

        List<CrawlItem> results = new ArrayList<>();
        int successfulCount = 0;
        int failedCount = 0;

        for (String url : urls) {
            if (!isValidUrl(url)) {
                results.add(new CrawlItem(url, false, "Invalid URL", null, "", 0, 0, 0, 0,0.0));
                failedCount++;
                continue;
            }

            try {
                Document doc = Jsoup.connect(url)
                        .timeout(timeout * 1000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        //.cache(!bypassCache)
                        .get();

                String title = doc.title();
                String content = convertToMarkdown(doc);
                int wordCount = content.isBlank() ? 0 : content.split("\\s+").length;
                int linksCount = doc.select("a[href]").size();
                int imagesCount = doc.select("img").size();

                if (wordCount >= wordCountThreshold) {
                    results.add(new CrawlItem(url, true, null, title, content, 200, wordCount, linksCount, imagesCount, 0.0));
                    successfulCount++;
                } else {
                    results.add(new CrawlItem(url, false, "Content below word count threshold", title, content, 200, wordCount, linksCount, imagesCount, 0.0));
                    failedCount++;
                }
            } catch (IOException e) {
                results.add(new CrawlItem(url, false, e.getMessage(), null, "", 0, 0, 0, 0, 0.0));
                failedCount++;
            }
        }

        return new CrawlResult(results, successfulCount, failedCount);
    }

    // 修复：删除无效的 .toURL() 冗余代码
    private boolean isValidUrl(String url) {
        try {
            new URI(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (URISyntaxException e) {
            return false;
        }
    }


    private String convertToMarkdown(Document doc) {
        StringBuilder markdown = new StringBuilder();
        Element body = doc.body();
        if (body == null) return "";
        parseElement(body, markdown);
        return markdown.toString();
    }


    private void parseElement(Element element, StringBuilder markdown) {
        markdown.append(elementToMarkdown(element));
        for (Element child : element.children()) {
            parseElement(child, markdown);
        }
    }


    private String elementToMarkdown(Element element) {
        String tagName = element.tagName().toLowerCase();

        return switch (tagName) {
            case "h1" -> "# " + element.text() + "\n\n";
            case "h2" -> "## " + element.text() + "\n\n";
            case "h3" -> "### " + element.text() + "\n\n";
            case "h4" -> "#### " + element.text() + "\n\n";
            case "h5" -> "##### " + element.text() + "\n\n";
            case "h6" -> "###### " + element.text() + "\n\n";
            case "p" -> element.text() + "\n\n";
            case "br" -> "\n";
            case "strong", "b" -> "**" + element.text() + "**";
            case "em", "i" -> "*" + element.text() + "*";
            case "a" -> "[" + element.text() + "](" + element.attr("href") + ")";
            case "ul" -> {
                StringBuilder sb = new StringBuilder();
                for (Element li : element.select("li")) {
                    sb.append("- ").append(li.text()).append("\n");
                }
                yield sb.toString();
            }
            case "ol" -> {
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (Element li : element.select("li")) {
                    sb.append(i++).append(". ").append(li.text()).append("\n");
                }
                yield sb.toString();
            }
            case "code" -> "`" + element.text() + "`";
            case "pre" -> "```\n" + element.text() + "\n```\n";
            case "blockquote" -> "> " + element.text() + "\n\n";
            case "hr" -> "---\n\n";
            default -> element.text();
        };
    }


    public record CrawlItem(
            String url,
            boolean success,
            String errorMessage,
            String title,
            String content,
            int statusCode,
            int wordCount,
            int linksCount,
            int imagesCount,
            double executionTime
    ) {}

    public record CrawlResult(
            List<CrawlItem> results,
            int successfulCount,
            int failedCount
    ) {}
}