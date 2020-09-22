import com.google.common.annotations.VisibleForTesting
import com.google.common.io.Files
import coza.opencollab.epub.creator.model.EpubBook
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Logger
import javax.imageio.ImageIO

class StoryDownloader(props: Properties) {
    val MAIN_URL = "https://author.today/"

    val LOGIN_XPATH = "/html/body/header/div/nav/ul/li[2]/a"
    val LOGIN_FIELD_XPATH = "/html/body/div[3]/div/div/div[2]/div/div/div/form/div[1]/input"
    val PASSWORD_FIELD_XPATH = "/html/body/div[3]/div/div/div[2]/div/div/div/form/div[2]/input"
    val LOGIN_BUTTON_XPATH = "/html/body/div[3]/div/div/div[2]/div/div/div/form/button"

    val TITLE_CSS_SELECTOR = "#text-container > h1"
    val STORY_NAME_CSS_SELECTOR = "#app > aside > div.book-row.no-border > div.book-row-content > div.book-title"
    val AUTHOR_NAME_CSS_SELECTOR = "#app > aside > div.book-row.no-border > div.book-row-content > div.book-author > a"

    val NEXT_CSS_SELECTOR = "#reader > div.reader-content.hidden-print > div > ul > li.next > a"

    private val wait: WebDriverWait

    @VisibleForTesting
    internal val driver: RemoteWebDriver
    private val atLogin: String
    private val atPassword: String
    private val baseDir: String

    private var storyName: String? = null
    private var authorName: String? = null

    private val logger = Logger.getLogger("StoryDownloader")

    private val chapters = mutableListOf<Chapter>()
        get() = field

    init {
        logger.info("Initializing StoryDownloader...")
        val chromeDriverPath = props.getProperty("chromedriver")

        System.setProperty("webdriver.chrome.driver", chromeDriverPath)
        val options = ChromeOptions()
        options.addArguments("--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors")
        driver = ChromeDriver(options)

        wait = WebDriverWait(driver, 10)

        atLogin = props.getProperty("login")
        atPassword = props.getProperty("password")
        baseDir = props.getProperty("basedir")

        logger.info("StoryDownloader initialized.")
    }

    fun closeWebdriver() {
        driver.close()
    }

    private fun setAttribute(element: WebElement?, attName: String?, attValue: String?) {
        logger.info("setAttribute: $attName=$attValue")
        driver.executeScript(
            "arguments[0].setAttribute(arguments[1], arguments[2]);",
            element, attName, attValue
        )
    }

    private fun checkPageIsLoaded(): Boolean {
        return ChromeDriverUtils.checkPageIsLoaded(driver)
    }

    fun downloadStory(startUrl: String): Int {
        assert(ensureLogin())
        driver.get(startUrl)

        storyName = driver.findElementByCssSelector(STORY_NAME_CSS_SELECTOR).text
        authorName = driver.findElementByCssSelector(AUTHOR_NAME_CSS_SELECTOR).text

        while (true) {
            chapters.add(getNextChapter())
            try {
                val elements = driver.findElementsByCssSelector(NEXT_CSS_SELECTOR)
                if (elements.isEmpty()) {
                    break
                }
                val element = elements[0]
                driver.executeScript("arguments[0].scrollIntoView(true);", element)
                element.click()
                Thread.sleep(100)
            } catch (e: Exception) {
                // Possibly last page?
                break
            }
        }

        return chapters.size
    }

    fun writeFiles() {
        assert(chapters.isNotEmpty())

        logger.info("Will be writing ${chapters.size} chapters to $baseDir")

        for (chapter in chapters) {
            logger.info("Processing chapter ${chapter.title}")
            val htmlFile = File("$baseDir/${chapter.title}.html")
            Files.createParentDirs(htmlFile)

            Files.write(chapter.text.toByteArray(), htmlFile)
            logger.info("Wrote ${htmlFile.absolutePath}")

            for (img in chapter.images) {
                val imgFile = File("$baseDir/${img.key}")
                Files.createParentDirs(imgFile)
                ImageIO.write(img.value, "jpg", imgFile)

                logger.info("Wrote ${imgFile.absolutePath}")
            }
        }
    }

    fun writeEpub(fileName: String) {
        assert(chapters.isNotEmpty())
        logger.info("Writing ebook by $authorName called $storyName")
        val book = EpubBook("ru", authorName + "_" + storyName, storyName, authorName)
        for (chapter in chapters) {
            logger.info("Processing chapter ${chapter.title}")

            book.addContent(chapter.text.toByteArray(),
                "application/xhtml+xml",
                chapter.title + ".html", true, true).id = chapter.title

            for (img in chapter.images) {
                val bos = ByteArrayOutputStream()
                ImageIO.write(img.value, "jpg", bos)
                book.addContent(bos.toByteArray(), "image/jpg", img.key, false, false)
            }
        }

        book.writeToFile(fileName)
    }

    @VisibleForTesting
    fun ensureLogin(): Boolean {
        driver.get(MAIN_URL)

        blockUntilPageIsLoaded()

        waitForXPath(LOGIN_XPATH)
        driver.findElement(By.ByXPath(LOGIN_XPATH)).click()

        waitForXPath(LOGIN_FIELD_XPATH)
        val login = driver.findElement(By.ByXPath(LOGIN_FIELD_XPATH))
        login.click()
        login.sendKeys(atLogin)

        waitForXPath(PASSWORD_FIELD_XPATH)
        login.sendKeys(Keys.TAB)
        val password = driver.findElement(By.ByXPath(PASSWORD_FIELD_XPATH))
        password.sendKeys(atPassword)

        driver.findElement(By.ByXPath(LOGIN_BUTTON_XPATH)).click()

        // Waiting for page to start reloading
        Thread.sleep(1000)

        // Waiting for network operations to finish
        waitForXPath(LOGIN_XPATH)

        return driver.manage().getCookieNamed("LoginCookie") != null
    }

    private fun blockUntilPageIsLoaded() {
        wait.until {
            checkPageIsLoaded()
        }
    }

    @VisibleForTesting
    fun getNextChapter(): Chapter {
        logger.info("Url: ${driver.currentUrl}")

        wait.until {
            checkPageIsLoaded()
        }

        // Sometimes we need to wait for CSS selector to render
        waitForCss(TITLE_CSS_SELECTOR)

        val title = driver.findElement(By.cssSelector(TITLE_CSS_SELECTOR)).getAttribute("innerText")

        val chapter = Chapter(title)
        val elements = driver.findElements(By.cssSelector("div.text-container > *"))
        chapter.text = "<!-- ${driver.currentUrl} -->\n"
        for (element in elements) {
            val images = element.findElements(By.cssSelector("img"))
            for (img in images) {
                val src = img.getAttribute("src")
                val url = URL(src)

                val bi = ImageIO.read(url)
                val fileName = "$baseDir/${url.file}"
                chapter.addImage(fileName, bi)
                setAttribute(img, "src", fileName)
            }

            var outerHTML = element.getAttribute("outerHTML")
            chapter.text += "$outerHTML\n"
        }

        val html = Jsoup.clean(chapter.text, Whitelist.basicWithImages())
        val doc = Jsoup.parse(html)
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .prettyPrint(true)
            .charset(StandardCharsets.UTF_8)

        chapter.text = doc.html()
        return chapter
    }

    private fun waitForXPath(xpath: String) {
        wait.until(ExpectedConditions.elementToBeClickable(By.ByXPath(xpath)))
    }

    private fun waitForCss(cssSelector: String) {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)))
    }
}