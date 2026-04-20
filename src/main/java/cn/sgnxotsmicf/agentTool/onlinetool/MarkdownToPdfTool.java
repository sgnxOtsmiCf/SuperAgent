
package cn.sgnxotsmicf.agentTool.onlinetool;

import cn.hutool.core.util.StrUtil;
import cn.sgnxotsmicf.common.tools.MinioUtil;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 企业级 Markdown 转 PDF 工具
 * 适配 Spring AI Tool 规范，提供完整的转换、渲染、上传闭环能力。
 * 特性：支持中文字体防乱码注入、A4打印格式化、代码块与表格美化、配置外部化。
 */
@Slf4j
@Component
public class MarkdownToPdfTool {

    @org.springframework.beans.factory.annotation.Autowired
    private MinioUtil minioUtil;

    @Value("${pdf.font.path:/fonts/SimSun.ttf}")
    private String fontPath;

    @Value("${pdf.font.family:SimSun}")
    private String fontFamily;

    @Value("${pdf.default-filename:generated_document.pdf}")
    private String defaultFileName;

    private byte[] fontBytesCache;

    /**
     * 初始化阶段预加载字体字节流，避免并发渲染时重复读取IO及流关闭异常
     */
    @PostConstruct
    public void initFontCache() {
        try (InputStream fontStream = this.getClass().getResourceAsStream(fontPath)) {
            if (fontStream != null) {
                this.fontBytesCache = fontStream.readAllBytes();
                log.info("成功预加载中文字体: {}", fontPath);
            } else {
                log.warn("未找到外部字体文件 {}，PDF可能无法正常显示中文", fontPath);
            }
        } catch (IOException e) {
            log.error("加载字体文件失败", e);
        }
    }

    /**
     * 将Markdown内容转换为格式化的PDF文档，上传至MinIO并返回临时下载地址
     *
     * @param markdownContent 待转换的Markdown文本内容
     * @param outputFileName  输出的PDF文件名
     * @return MinIO上的文件预览/下载地址
     */
    @Tool(description = "Converts Markdown content into a formatted PDF document, uploads it to MinIO, and returns a temporary download URL.")
    public String convertMarkdownToPdf(
            @ToolParam(description = "The Markdown content to be converted to PDF") String markdownContent,
            @ToolParam(description = "The desired output file name for the PDF document") String outputFileName) {

        Assert.hasText(markdownContent, "Markdown content must not be empty");

        try {
            // 1. Markdown 转 HTML
            String htmlContent = convertMarkdownToHtml(markdownContent);

            // 2. 包装完整 HTML 文档结构及企业级样式
            String fullHtml = wrapHtmlWithStyle(htmlContent);

            // 3. HTML 渲染为 PDF
            byte[] pdfBytes = renderHtmlToPdf(fullHtml);

            // 4. 文件名规范化
            String fileName = StrUtil.isBlank(outputFileName) ? defaultFileName : outputFileName;
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }

            // 5. 上传并返回地址
            return minioUtil.uploadBytes(pdfBytes, fileName, "application/pdf");
        } catch (Exception e) {
            log.error("Markdown转PDF并上传MinIO失败", e);
            throw new IllegalStateException("Failed to generate or upload PDF: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Markdown为HTML片段
     */
    private String convertMarkdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * 注入企业级CSS样式：A4打印边距、中文字体栈、防断行表格、代码块高亮美化
     */
    private String wrapHtmlWithStyle(String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        @page {
                            size: A4;
                            margin: 2cm;
                        }
                        body {
                            font-family: "%s", "Microsoft YaHei", "PingFang SC", sans-serif;
                            line-height: 1.6;
                            color: #333;
                            font-size: 12pt;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            color: #1a1a1a;
                            margin-top: 1.2em;
                            margin-bottom: 0.6em;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                            margin-bottom: 1em;
                            table-layout: fixed;
                            word-wrap: break-word;
                        }
                        th, td {
                            border: 1px solid #ddd;
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: #f8f9fa;
                            font-weight: bold;
                        }
                        code {
                            background-color: #f1f3f5;
                            padding: 2px 4px;
                            border-radius: 4px;
                            font-family: "Source Code Pro", "Consolas", monospace;
                            font-size: 0.9em;
                        }
                        pre {
                            background-color: #f1f3f5;
                            padding: 12px;
                            border-radius: 4px;
                            overflow-x: auto;
                            border: 1px solid #e9ecef;
                        }
                        pre code {
                            padding: 0;
                            background-color: transparent;
                            border: none;
                        }
                        blockquote {
                            border-left: 4px solid #007bff;
                            padding-left: 1em;
                            margin-left: 0;
                            color: #6c757d;
                        }
                        img {
                            max-width: 100%;
                            height: auto;
                        }
                    </style>
                </head>
                <body>
                """.formatted(fontFamily) + bodyHtml + """
                </body>
                </html>
                """;
    }

    /**
     * OpenHtmlToPdf 渲染引擎处理
     * 采用内存缓存字体字节流解决并发下流关闭与重复读取问题
     */
    private byte[] renderHtmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // 注入缓存中的字体字节流
            if (this.fontBytesCache != null) {
                builder.useFont(() -> new ByteArrayInputStream(this.fontBytesCache), this.fontFamily);
            }

            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}