package cn.sgnxotsmicf.agentTool.localtool;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class BrowserUseTool {

    private WebDriver driver;

    @Tool(description = "Navigate to a specific URL")
    public String goToUrl(@ToolParam(description = "The URL to navigate to") String url) {
        try {
            getDriver().get(url);
            return "Navigated to " + url;
        } catch (Exception e) {
            return "Error navigating to URL: " + e.getMessage();
        }
    }

    @Tool(description = "Click an element on the page")
    public String clickElement(@ToolParam(description = "CSS selector or XPath of the element to click") String selector) {
        try {
            WebDriver driver = getDriver();
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(createBy(selector)));
            element.click();
            return "Clicked element: " + selector;
        } catch (Exception e) {
            return "Error clicking element: " + e.getMessage();
        }
    }

    @Tool(description = "Input text into an element")
    public String inputText(
            @ToolParam(description = "CSS selector or XPath of the element") String selector,
            @ToolParam(description = "Text to input") String text
    ) {
        try {
            WebDriver driver = getDriver();
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(createBy(selector)));
            element.clear();
            element.sendKeys(text);
            return "Input text into element: " + selector;
        } catch (Exception e) {
            return "Error inputting text: " + e.getMessage();
        }
    }

    @Tool(description = "Get page title")
    public String getPageTitle() {
        try {
            return "Page title: " + getDriver().getTitle();
        } catch (Exception e) {
            return "Error getting page title: " + e.getMessage();
        }
    }

    @Tool(description = "Get page URL")
    public String getPageUrl() {
        try {
            return "Current URL: " + getDriver().getCurrentUrl();
        } catch (Exception e) {
            return "Error getting page URL: " + e.getMessage();
        }
    }

    @Tool(description = "Extract text from an element")
    public String getElementText(@ToolParam(description = "CSS selector or XPath of the element") String selector) {
        try {
            WebDriver driver = getDriver();
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(createBy(selector)));
            return "Element text: " + element.getText();
        } catch (Exception e) {
            return "Error getting element text: " + e.getMessage();
        }
    }

    @Tool(description = "Go back in browser history")
    public String goBack() {
        try {
            getDriver().navigate().back();
            return "Navigated back";
        } catch (Exception e) {
            return "Error navigating back: " + e.getMessage();
        }
    }

    @Tool(description = "Refresh the current page")
    public String refreshPage() {
        try {
            getDriver().navigate().refresh();
            return "Page refreshed";
        } catch (Exception e) {
            return "Error refreshing page: " + e.getMessage();
        }
    }

    @Tool(description = "Close the browser")
    public String closeBrowser() {
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
            return "Browser closed";
        } catch (Exception e) {
            return "Error closing browser: " + e.getMessage();
        }
    }

    /**
     * 自动判断选择器类型（CSS/XPath），保持原有英文描述不变
     */
    private By createBy(String selector) {
        if (selector.startsWith("//")) {
            return By.xpath(selector);
        }
        return By.cssSelector(selector);
    }

    /**
     * 初始化浏览器驱动，优化环境兼容性
     */
    private WebDriver getDriver() {
        if (driver == null) {
            ChromeOptions options = new ChromeOptions();
            // 无头模式（保留原有配置）
            options.addArguments("--headless");
            // 修复Linux/Docker环境启动失败
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            // 禁用日志干扰
            options.addArguments("--disable-logging");
            driver = new ChromeDriver(options);
            // 最大化窗口，避免布局异常
            driver.manage().window().maximize();
        }
        return driver;
    }

    /**
     * Spring容器销毁时自动关闭浏览器，防止进程残留
     */
    @PreDestroy
    public void preDestroy() {
        this.closeBrowser();
    }
}