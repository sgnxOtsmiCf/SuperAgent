package cn.sgnxotsmicf.agentTool.onlinetool;/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * A smart web fetch tool that retrieves content from URLs and processes it using an AI
 * model for summarization.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Fetches HTML content and converts it to Markdown</li>
 * <li>Includes a 15-minute cache for faster repeated access</li>
 * <li>Optional domain safety checking via Claude's domain info API</li>
 * <li>Automatic content truncation with configurable limits</li>
 * </ul>
 *
 * <p>
 * This class implements {@link AutoCloseable} to ensure proper cleanup of HTTP client
 * resources. It's recommended to use try-with-resources or explicitly call
 * {@link #close()} when done using this tool.
 *
 * @author Christian Tzolov
 * @see <a href="https://mikhail.io/2025/10/claude-code-web-tools/">Reference</a>
 */
public class SmartWebFetchTool implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(SmartWebFetchTool.class);

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private static final Duration CACHE_TTL = Duration.ofMinutes(15);

	private static final String DOMAIN_SAFETY_CHECK_URL = "https://claude.ai/api/web/domain_info";

	private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([^;\\s]+)", Pattern.CASE_INSENSITIVE);

	private static final String FETCH_SUMMARIZE_PROMPT = """
			Web page content:
			---
			{content}
			---

			{userQuery}

			Provide a concise response based only on the content above. In your response:
			- Enforce a strict 125-character maximum for quotes from any source document. Open Source Software is ok as long as we respect the license.
			- Use quotation marks for exact language from articles; any language outside of the quotation should never be word-for-word the same.
			- You are not a lawyer and never comment on the legality of your own prompts and responses.
			- Never produce or reproduce exact song lyrics.
			""";

	private final HttpClient httpClient;

	private final ChatClient chatClient;

	private final int maxContentLength;

	private final boolean domainSafetyCheck;

	private final DefaultDomainCanFetchChecker domainCanFetchChecker;

	private final FlexmarkHtmlConverter htmlToMarkdownConverter;

	private final Map<String, CacheEntry> urlCache;

	private final int maxCacheSize;

	private final Object cacheLock = new Object();

	private final boolean failOpenOnSafetyCheckError;

	private final int maxRetries;

	private SmartWebFetchTool(ChatClient chatClient, int maxContentLength, boolean domainSafetyCheck, int maxCacheSize,
							  boolean failOpenOnSafetyCheckError, int maxRetries) {
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.ALWAYS)
				.connectTimeout(DEFAULT_CONNECT_TIMEOUT)
				.build();

		this.chatClient = chatClient;
		this.maxContentLength = maxContentLength;
		this.domainSafetyCheck = domainSafetyCheck;
		this.maxCacheSize = maxCacheSize;
		this.failOpenOnSafetyCheckError = failOpenOnSafetyCheckError;
		this.maxRetries = maxRetries;
		this.htmlToMarkdownConverter = FlexmarkHtmlConverter.builder().build();
		this.domainCanFetchChecker = new DefaultDomainCanFetchChecker();
		this.urlCache = new ConcurrentHashMap<>();
	}

	// @formatter:off
	@Tool(name = "WebFetch", description = """
		Fetches content from a specified URL and processes it using an AI model.

		Features:
		- Takes a URL and a prompt as input
		- Fetches the URL content using HTTP GET method
		- Converts HTML to markdown
		- Processes the content with the prompt using a small, fast model
		- Returns the model's response about the content
		- Includes a self-cleaning 15-minute cache for faster responses
		- Automatic retry on network errors and 5xx server errors

		Usage notes:
		- IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead.
		- The URL must be a fully-formed valid URL (e.g., https://example.com)
		- HTTP URLs will be automatically upgraded to HTTPS
		- Only HTTP GET requests are supported (read-only)
		- The prompt should describe what information you want to extract from the page
		- This tool is read-only and does not modify any files or send any data
		- Results may be summarized if the content is very large
		- Retries up to 2 times (configurable) on transient failures with exponential backoff
		""")
	// @formatter:on
	public String webFetch(@ToolParam(description = "The URL to fetch content from") String url,
						   @ToolParam(description = "The prompt to run on the fetched content") String prompt,
						   ToolContext toolContext) {

		if (!StringUtils.hasText(url)) {
			return "Error: URL cannot be empty or null";
		}

		url = url.trim();

		URI uri;
		try {
			uri = URI.create(url);
			if (uri.getScheme() == null || uri.getHost() == null) {
				return "Error: Invalid URL format. Please provide a fully-formed URL (e.g., https://example.com)";
			}
		}
		catch (IllegalArgumentException e) {
			return "Error: Invalid URL format: " + e.getMessage();
		}

		if (this.domainSafetyCheck) {
			DomainCanFetch check = this.domainCanFetchChecker.check(url, this.failOpenOnSafetyCheckError);
			if (!check.canFetch()) {
				return "Domain safety check failed for URL '" + url + "': " + check.reason();
			}
		}

		String cacheKey = this.buildCacheKey(url, prompt, toolContext);
		String content = this.getCachedContent(cacheKey);

		if (content != null) {
			logger.debug("Cache hit for URL: {} with prompt hash: {}", url, prompt.hashCode());
			return content;
		}

		logger.debug("Cache miss for URL: {} with prompt hash: {}", url, prompt.hashCode());

		String htmlContent;
		try {
			HttpResponse<String> response = this.fetchHtmlWithRetry(url);
			if (response.statusCode() >= 400) {
				return "Error: Failed to fetch URL. HTTP status code: " + response.statusCode();
			}
			htmlContent = response.body();
			if (htmlContent == null || htmlContent.isBlank()) {
				return "Error: Retrieved empty content from URL";
			}
		}
		catch (WebFetchException e) {
			logger.error("Failed to fetch URL: {}", url, e);
			return "Error fetching URL: " + e.getMessage();
		}

		String mdContent = this.htmlToMarkdownConverter.convert(htmlContent);
		mdContent = this.truncate(mdContent);
		String summary = this.summarize(mdContent, prompt);
		this.cacheContent(cacheKey, summary);

		return summary;
	}

	private String buildCacheKey(String url, String prompt, ToolContext toolContext) {
		Object userIdObj = toolContext.getContext().get("userId");
		String userId = (userIdObj != null) ? userIdObj.toString() : "global";
		return userId + "::" + url + "::prompt::" + prompt.hashCode();
	}

	private HttpResponse<String> fetchHtmlWithRetry(String url) {
		int attempt = 0;
		Exception lastException = null;

		while (attempt <= this.maxRetries) {
			try {
				if (attempt > 0) {
					long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
					logger.debug("Retrying fetch for URL: {} (attempt {}/{}), waiting {}ms", url, attempt,
							this.maxRetries, backoffMs);
					Thread.sleep(backoffMs);
				}

				HttpResponse<String> response = this.fetchHtml(url);

				if (response.statusCode() >= 500 && response.statusCode() < 600) {
					lastException = new WebFetchException(
							"Server error: HTTP " + response.statusCode(), null);
					logger.warn("Fetch attempt {} returned server error {} for URL: {}",
							attempt + 1, response.statusCode(), url);
					attempt++;
					continue;
				}

				return response;
			}
			catch (WebFetchException e) {
				lastException = e;
				if (e.getCause() instanceof InterruptedException) {
					throw e;
				}
				logger.warn("Fetch attempt {} failed for URL: {}: {}", attempt + 1, url, e.getMessage());
				attempt++;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new WebFetchException("Retry interrupted", e);
			}
		}

		if (lastException == null) {
			throw new WebFetchException("Failed after " + (this.maxRetries + 1) + " attempts", null);
		}
		else if (lastException instanceof WebFetchException) {
			throw new WebFetchException("Failed after " + (this.maxRetries + 1) + " attempts", lastException);
		}
		else {
			throw new WebFetchException("Failed after " + (this.maxRetries + 1) + " attempts: "
					+ lastException.getMessage(), lastException);
		}
	}

	private HttpResponse<String> fetchHtml(String url) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(DEFAULT_REQUEST_TIMEOUT)
				.header("User-Agent", USER_AGENT)
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.5")
				.GET()
				.build();

		try {
			HttpResponse<byte[]> byteResponse = this.httpClient.send(request,
					HttpResponse.BodyHandlers.ofByteArray());

			Charset charset = this.extractCharset(byteResponse).orElse(StandardCharsets.UTF_8);
			String body = new String(byteResponse.body(), charset);

			return new HttpResponse<String>() {
				@Override
				public int statusCode() {
					return byteResponse.statusCode();
				}

				@Override
				public HttpRequest request() {
					return byteResponse.request();
				}

				@Override
				public Optional<HttpResponse<String>> previousResponse() {
					return Optional.empty();
				}

				@Override
				public java.net.http.HttpHeaders headers() {
					return byteResponse.headers();
				}

				@Override
				public String body() {
					return body;
				}

				@Override
				public Optional<javax.net.ssl.SSLSession> sslSession() {
					return byteResponse.sslSession();
				}

				@Override
				public URI uri() {
					return byteResponse.uri();
				}

				@Override
				public java.net.http.HttpClient.Version version() {
					return byteResponse.version();
				}
			};
		}
		catch (IOException e) {
			throw new WebFetchException("Network error while fetching URL: " + e.getMessage(), e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WebFetchException("Request was interrupted", e);
		}
	}

	private Optional<Charset> extractCharset(HttpResponse<?> response) {
		return response.headers()
				.firstValue("Content-Type")
				.flatMap(contentType -> {
					Matcher matcher = CHARSET_PATTERN.matcher(contentType);
					if (matcher.find()) {
						String charsetName = matcher.group(1);
						try {
							return Optional.of(Charset.forName(charsetName));
						}
						catch (Exception e) {
							logger.warn("Unsupported charset '{}', falling back to UTF-8", charsetName);
							return Optional.empty();
						}
					}
					return Optional.empty();
				});
	}

	private String summarize(String content, String userQuery) {
		try {
			String response = this.chatClient.prompt()
					.user(u -> u.text(FETCH_SUMMARIZE_PROMPT).param("content", content).param("userQuery", userQuery))
					.call()
					.content();
			return response != null ? response : "Error: Received empty response from AI model";
		}
		catch (Exception e) {
			logger.error("Failed to summarize content", e);
			return "Error summarizing content: " + e.getMessage();
		}
	}

	private String truncate(String content) {
		if (content == null) {
			return "";
		}
		if (content.length() > this.maxContentLength) {
			logger.warn("Content too long ({} characters). Truncating to {} characters.", content.length(),
					this.maxContentLength);
			return content.substring(0, this.maxContentLength);
		}
		return content;
	}

	private String getCachedContent(String url) {
		CacheEntry entry = this.urlCache.get(url);
		if (entry != null && !entry.isExpired()) {
			return entry.content();
		}
		if (entry != null) {
			this.urlCache.remove(url);
		}
		return null;
	}

	private void cacheContent(String cacheKey, String content) {
		if (this.urlCache.size() > this.maxCacheSize) {
			synchronized (this.cacheLock) {
				if (this.urlCache.size() > this.maxCacheSize) {
					this.cleanExpiredEntries();
				}
			}
		}
		this.urlCache.put(cacheKey, new CacheEntry(content, System.currentTimeMillis()));
	}

	private void cleanExpiredEntries() {
		this.urlCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
		logger.debug("Cleaned up expired cache entries. Current cache size: {}", this.urlCache.size());
	}

	@Override
	public void close() {
		this.urlCache.clear();
		logger.debug("SmartWebFetchTool closed and resources cleaned up");
	}

	private record CacheEntry(String content, long timestamp) {
		boolean isExpired() {
			return System.currentTimeMillis() - timestamp > CACHE_TTL.toMillis();
		}
	}

	public record DomainCanFetch(String domain, boolean canFetch, String reason) {
	}

	public static class WebFetchException extends RuntimeException {

		public WebFetchException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	private static class DefaultDomainCanFetchChecker {

		private final RestClient restClient;

		public DefaultDomainCanFetchChecker() {
			this.restClient = RestClient.builder().baseUrl(DOMAIN_SAFETY_CHECK_URL).build();
		}

		public DomainCanFetch check(String url, boolean failOpenOnError) {
			String domain;
			try {
				domain = URI.create(url).getHost();
				if (domain == null) {
					return new DomainCanFetch(url, false, "Could not extract domain from URL");
				}
			}
			catch (IllegalArgumentException e) {
				return new DomainCanFetch(url, false, "Invalid URL format: " + e.getMessage());
			}

			try {
				ResponseEntity<DomainSafetyResponse> response = this.checkDomainSafety(domain);

				if (!response.hasBody()) {
					return new DomainCanFetch(domain, false,
							"Failed to check domain safety. Status: " + response.getStatusCode());
				}

				DomainSafetyResponse body = response.getBody();
				if (body == null || body.can_fetch() != Boolean.TRUE) {
					return new DomainCanFetch(domain, false, "The domain is not safe to fetch content from.");
				}

				return new DomainCanFetch(domain, true, "Domain is safe to fetch.");
			}
			catch (Exception e) {
				logger.warn("Failed to check domain safety for {}: {}", domain, e.getMessage());
				if (failOpenOnError) {
					return new DomainCanFetch(domain, true, "Safety check unavailable, proceeding with fetch.");
				}
				else {
					return new DomainCanFetch(domain, false,
							"Safety check failed: " + e.getMessage() + ". Blocking fetch for security.");
				}
			}
		}

		private record DomainSafetyResponse(String domain, Boolean can_fetch) {
		}

		private ResponseEntity<DomainSafetyResponse> checkDomainSafety(String domain) {
			return this.restClient.get()
					.uri(uriBuilder -> uriBuilder.queryParam("domain", domain).build())
					.retrieve()
					.toEntity(DomainSafetyResponse.class);
		}

	}

	public static Builder builder(ChatClient chatClient) {
		return new Builder(chatClient);
	}

	public static class Builder {

		private final ChatClient chatClient;

		private int maxContentLength = 100_000;

		private boolean domainSafetyCheck = true;

		private int maxCacheSize = 100;

		private boolean failOpenOnSafetyCheckError = true;

		private int maxRetries = 2;

		private Builder(ChatClient chatClient) {
			if (chatClient == null) {
				throw new IllegalArgumentException("ChatClient must not be null");
			}
			this.chatClient = chatClient;
		}

		public Builder maxContentLength(int maxContentLength) {
			if (maxContentLength <= 0) {
				throw new IllegalArgumentException("maxContentLength must be positive");
			}
			this.maxContentLength = maxContentLength;
			return this;
		}

		public Builder domainSafetyCheck(boolean domainSafetyCheck) {
			this.domainSafetyCheck = domainSafetyCheck;
			return this;
		}

		public Builder maxCacheSize(int maxCacheSize) {
			if (maxCacheSize <= 0) {
				throw new IllegalArgumentException("maxCacheSize must be positive");
			}
			this.maxCacheSize = maxCacheSize;
			return this;
		}

		public Builder failOpenOnSafetyCheckError(boolean failOpenOnSafetyCheckError) {
			this.failOpenOnSafetyCheckError = failOpenOnSafetyCheckError;
			return this;
		}

		public Builder maxRetries(int maxRetries) {
			if (maxRetries < 0) {
				throw new IllegalArgumentException("maxRetries cannot be negative");
			}
			this.maxRetries = maxRetries;
			return this;
		}

		public SmartWebFetchTool build() {
			return new SmartWebFetchTool(this.chatClient, this.maxContentLength, this.domainSafetyCheck,
					this.maxCacheSize, this.failOpenOnSafetyCheckError, this.maxRetries);
		}

	}

}
