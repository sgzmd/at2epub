import com.google.common.io.Files
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.net.URL
import java.util.*
import java.util.logging.Logger
import javax.imageio.ImageIO


object Main {
    val MAIN_URL = "https://author.today/"
    val STORY_URL = "https://author.today/reader/45969/361004"
    val logger = Logger.getLogger("Main")


    val next =     "#reader > div.reader-content.hidden-print > div > ul > li.next > a"

    @JvmStatic
    fun main(args: Array<String>) {
        main()
    }

    fun setAttribute(driver: RemoteWebDriver, element: WebElement?, attName: String?, attValue: String?) {
        driver.executeScript(
            "arguments[0].setAttribute(arguments[1], arguments[2]);",
            element, attName, attValue
        )
    }

    private fun main() {
        val props = Properties()
        props.load(FileInputStream("login.properties"))


        val chromeDriverPath = "chromedriver/chromedriver.exe"
        System.setProperty("webdriver.chrome.driver", chromeDriverPath)
        val options = ChromeOptions()
        options.addArguments("--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors")
        val driver: WebDriver = ChromeDriver(options)

        driver.get(MAIN_URL)
        val wait = WebDriverWait(driver, 10)

        val LOGIN_XPATH = "/html/body/header/div/nav/ul/li[2]/a"
        waitFor(wait, LOGIN_XPATH)
        driver.findElement(By.ByXPath(LOGIN_XPATH)).click()
        //        Thread.sleep(1000)

        val LOGIN_FIELD_XPATH = "/html/body/div[3]/div/div/div[2]/div/div/div/form/div[1]/input"
        waitFor(wait, LOGIN_FIELD_XPATH)
        val login = driver.findElement(By.ByXPath(LOGIN_FIELD_XPATH))
        login.click()
        login.sendKeys(props.getProperty("login"))
        val PASSWORD_FIELD_XPATH = "/html/body/div[3]/div/div/div[2]/div/div/div/form/div[2]/input"
        waitFor(wait, PASSWORD_FIELD_XPATH)
        login.sendKeys(Keys.TAB)
        val password = driver.findElement(By.ByXPath(PASSWORD_FIELD_XPATH))
        password.sendKeys(props.getProperty("password"))
        driver.findElement(By.ByXPath("/html/body/div[3]/div/div/div[2]/div/div/div/form/button")).click()

        driver.get(STORY_URL)
        while (true) {
            extractChapter(driver)
            try {
                val element = driver.findElement(By.ByCssSelector(next))
                element.click()
                Thread.sleep(100)
                waitFor(wait, "/html/body/div[1]/section/div[2]/div")
            } catch (e: NoSuchElementException) {
                break
            }
        }


        driver.close()
    }

    private fun extractChapter(driver: WebDriver) {
        val elements = driver.findElements(By.cssSelector("div.text-container > *"))
        var text = ""
        for (element in elements) {
            logger.info(element.getAttribute("innerHTML"))

            val images = element.findElements(By.cssSelector("img"))
            for (img in images) {
                val src = img.getAttribute("src")
                logger.info("Image: %s".format(src))
                val url = URL(src)

                val bi = ImageIO.read(url)
                val outFile = File("./" + url.file)
                Files.createParentDirs(outFile)
                ImageIO.write(bi, "jpg", outFile)
                logger.info("Created %s".format(outFile.absolutePath))

                setAttribute(driver as RemoteWebDriver, img, "src", outFile.path)
            }

            var innerHTML = element.getAttribute("innerHTML")

            text = text + "<div>" + innerHTML + "</div>"
        }

        val result = File("./" + driver.title + ".html")
        BufferedWriter(FileWriter(result)).write(text)
    }

    private fun waitFor(wait: WebDriverWait, xpath: String) {
        wait.until(ExpectedConditions.elementToBeClickable(By.ByXPath(xpath)))
    }


}