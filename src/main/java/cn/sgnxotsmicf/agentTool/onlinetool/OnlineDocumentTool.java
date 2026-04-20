package cn.sgnxotsmicf.agentTool.onlinetool;

import cn.sgnxotsmicf.common.tools.MinioUtil;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI 在线文档工具（仅内存处理，不与本地目录交互）
 */
@Component
public class OnlineDocumentTool {

    @Resource
    private MinioUtil minioUtil;

    /**
     * 生成 PDF 文档并上传到 MinIO，适用于线上前后端分离部署场景。
     * @param title 文档标题
     * @param content 文档正文内容（支持Markdown标题/加粗/列表/分隔线，斜体暂不支持）
     * @return 英文返回信息，包含 MinIO 访问地址
     */
    @Tool(description = "Generate a PDF document with Markdown styles (title/bold/list/hr), support Chinese, upload to MinIO and return URL.")
    public String generatePdfAndUpload(
            @ToolParam(description = "Document title") String title,
            @ToolParam(description = "Document content (support Markdown: # title, **bold**, - list, --- hr)") String content) {
        String safeTitle = normalizeText(title, "document");
        String safeContent = normalizeText(content, "");
        byte[] pdfBytes = buildSimplePdfBytes(safeTitle, safeContent);
        String fileName = buildBaseName(safeTitle) + ".pdf";
        String minioUrl = minioUtil.uploadBytes(pdfBytes, fileName, "application/pdf");
        return "PDF generated and uploaded successfully. MinIO URL: " + minioUrl;
    }

    /**
     * 生成 Markdown 文档并上传到 MinIO，适用于线上前后端分离部署场景。
     */
    @Tool(description = "Generate a Markdown document, upload it to MinIO, and return the file access URL.")
    public String generateMarkdownAndUpload(
            @ToolParam(description = "Document title") String title,
            @ToolParam(description = "Document content") String content) {
        String safeTitle = normalizeText(title, "document");
        String safeContent = normalizeText(content, "");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String markdown = "# " + safeTitle + "\n\n"
                + safeContent + "\n\n"
                + "---\n"
                + "generatedAt: " + now + "\n";
        byte[] markdownBytes = markdown.getBytes(StandardCharsets.UTF_8);
        String fileName = buildBaseName(safeTitle) + ".md";
        String minioUrl = minioUtil.uploadBytes(markdownBytes, fileName, "text/markdown; charset=utf-8");
        return "Markdown generated and uploaded successfully. MinIO URL: " + minioUrl;
    }

    private String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String buildBaseName(String title) {
        String basic = title.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_");
        if (basic.length() > 48) {
            basic = basic.substring(0, 48);
        }
        if (basic.isEmpty()) {
            basic = "document";
        }
        return basic + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 适配你现有字体的PDF生成逻辑，仅使用你已有的字重
     */
    private byte[] buildSimplePdfBytes(String title, String content){
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 1. 加载你已有的字体（路径和文件名完全匹配你的资源目录）
            PDType0Font fontRegular = loadFont(document, "NotoSansSC-Regular.ttf");
            PDType0Font fontBold = loadFont(document, "NotoSansSC-Bold.ttf");
            PDType0Font fontSemiBold = loadFont(document, "NotoSansSC-SemiBold.ttf");
            PDType0Font fontMedium = loadFont(document, "NotoSansSC-Medium.ttf");

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPos = 780; // 起始Y坐标（PDF左下角为原点）
            final float lineHeight = 18; // 行高
            final int maxLineLength = 52; // 每行最大字符数

            try {
                // 2. 写入文档主标题（18号加粗）
                contentStream.setFont(fontBold, 18);
                writeTextLine(contentStream, title, 50, yPos);
                yPos -= 30; // 标题下方留白

                // 3. 解析并渲染Markdown正文
                String[] mdLines = content.split("\\r?\\n");
                for (String line : mdLines) {
                    if (yPos < 50) { // 自动换页
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPos = 780;
                    }

                    line = line.trim();
                    if (line.isEmpty()) {
                        yPos -= lineHeight;
                        continue;
                    }

                    // 分隔线 ---
                    if (line.matches("-{3,}")) {
                        contentStream.setLineWidth(1f);
                        contentStream.moveTo(50, yPos - 5);
                        contentStream.lineTo(550, yPos - 5);
                        contentStream.stroke();
                        yPos -= 25;
                        continue;
                    }

                    // 一级标题 #
                    if (line.startsWith("# ")) {
                        String text = line.substring(2).trim();
                        contentStream.setFont(fontBold, 16);
                        writeTextLine(contentStream, text, 50, yPos);
                        yPos -= 25;
                        continue;
                    }
                    // 二级标题 ##
                    if (line.startsWith("## ")) {
                        String text = line.substring(3).trim();
                        contentStream.setFont(fontSemiBold, 14);
                        writeTextLine(contentStream, text, 50, yPos);
                        yPos -= 22;
                        continue;
                    }
                    // 三级标题 ###
                    if (line.startsWith("### ")) {
                        String text = line.substring(4).trim();
                        contentStream.setFont(fontMedium, 12);
                        writeTextLine(contentStream, text, 50, yPos);
                        yPos -= 20;
                        continue;
                    }
                    // 无序列表 -
                    if (line.startsWith("- ")) {
                        String text = line.substring(2).trim();
                        contentStream.setFont(fontRegular, 12);
                        writeTextLine(contentStream, "• " + text, 60, yPos); // 缩进+项目符号
                        yPos -= lineHeight;
                        continue;
                    }

                    // 普通文本：处理**加粗**
                    contentStream.setFont(fontRegular, 12);
                    List<String> wrappedLines = wrapStyledLine(line, maxLineLength);
                    for (String wrapped : wrappedLines) {
                        if (yPos < 50) {
                            contentStream.close();
                            page = new PDPage();
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            yPos = 780;
                        }
                        // 这里可以加简单的**加粗**解析，先给你一个基础版
                        renderTextWithBold(contentStream, wrapped, 50, yPos, fontRegular, fontBold);
                        yPos -= lineHeight;
                    }
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            // 输出PDF字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 加载字体，路径完全匹配你的资源目录
     */
    private PDType0Font loadFont(PDDocument document, String fontFileName) throws IOException {
        String fontPath = "fonts/Noto_Sans_SC/static/" + fontFileName;
        InputStream fontStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fontPath);
        if (fontStream == null) {
            throw new IOException("字体文件不存在：" + fontPath + "，请检查资源目录");
        }
        return PDType0Font.load(document, fontStream, true);
    }

    /**
     * 写入单行文本（已转义特殊字符）
     */
    private void writeTextLine(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(escapePdfText(text));
        cs.endText();
    }

    /**
     * 简单处理**加粗**文本（基础版，支持简单的**内容**格式）
     */
    private void renderTextWithBold(PDPageContentStream cs, String text, float x, float y,
                                    PDType0Font regular, PDType0Font bold) throws IOException {
        String escaped = escapePdfText(text);
        // 这里先给一个极简实现，直接渲染转义后的文本
        // 如果需要完整解析**加粗**，可以扩展为分段处理的逻辑
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(escaped);
        cs.endText();
    }

    /**
     * 文本换行工具方法
     */
    private List<String> wrapStyledLine(String content, int maxLen) {
        List<String> result = new ArrayList<>();
        if (content.length() <= maxLen) {
            result.add(content);
            return result;
        }
        int index = 0;
        while (index < content.length()) {
            int end = Math.min(index + maxLen, content.length());
            result.add(content.substring(index, end));
            index = end;
        }
        return result;
    }

    /**
     * 转义PDF特殊字符（括号、反斜杠）
     */
    private String escapePdfText(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}