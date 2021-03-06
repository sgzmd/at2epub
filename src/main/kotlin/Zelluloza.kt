import ChromeDriverUtils.blockUntilLoaded
import ChromeDriverUtils.scrollIntoView
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.OutputType
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.FileInputStream
import java.lang.Exception
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

class Zelluloza(
  val storyUrl: String,
  val userName: String,
  val password: String,
  val chromedriverPath: String,
  val basedir: String
) {

  private val logger = Logger.getLogger("ChromeDriverUtils")

  private var driver: ChromeDriver
  private var wait: WebDriverWait
  private var screenshotId = 1

  private var autoAdjusted = false

  private val ACTIVE_CANVAS_SELECTOR = "canvas[style='display: inline;']"

  init {
    System.setProperty("webdriver.chrome.driver", chromedriverPath)
    val options = ChromeOptions()
    options.addArguments("--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors")

    driver = ChromeDriver(options)
    wait = WebDriverWait(driver, 10)
  }

  private fun getAndWait(url: String) {
    driver.get(url)
    blockUntilLoaded(wait)
  }

  /**
   * Attempts to sign in to Zelluloza
   */
  private fun performLogin(): Boolean {
    getAndWait("https://zelluloza.ru/my/")

    driver.findElement(By.cssSelector("#email")).sendKeys(userName)
    driver.findElement(By.cssSelector("#bodyobj > div:nth-child(5) > div.cnt > div.cntl > main > form > input:nth-child(6)"))
      .sendKeys(password)

    // Pressing submit
    driver.findElement(By.cssSelector("#bodyobj > div:nth-child(5) > div.cnt > div.cntl > main > form > div:nth-child(8) > input"))
      .click()

    blockUntilLoaded(wait)

    try {
      driver.findElement(By.cssSelector("#dropdown07"))
      return true
    } catch (e: NoSuchElementException) {
      return false
    }
  }

  private fun writeScreenshot(screenshot: ByteArray?) {
    logger.info("Got a screenshot, ${screenshot?.size} bytes.")


    val screenshotIdPostfix = screenshotId.toString().padStart(5, '0')
    val basedirPath = Paths.get(basedir)
    if (!Files.exists(basedirPath)) {
      Files.createDirectory(basedirPath)
    }

    val fos = Files.newOutputStream(Paths.get(basedirPath.toString(), "screenshot_$screenshotIdPostfix.png"))
    fos.use {
      it.write(screenshot)
    }

    ++screenshotId
  }
  fun getScreenshot(): ByteArray? {
    wait.until { driver.findElementsByCssSelector(ACTIVE_CANVAS_SELECTOR).size > 0 }

    val dataUrl = driver.executeScript("return document.querySelector(\"$ACTIVE_CANVAS_SELECTOR\").toDataURL('image/png')")
    // URL will look like this: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA .... "

    val dataUrlString = dataUrl.toString()
    return Base64.getDecoder().decode(dataUrlString.split(',')[1])
  }

  fun scrollBackToStartOfFragment() {
    logger.info("Scrolling back just in case, hang on... ")
    for (i in 0 until 50) {
      driver.executeScript("return bpage(0,1);")
      Thread.sleep(30)
    }

    Thread.sleep(1000)
  }

  fun readFragment() {
    val fragmentUrl = driver.currentUrl
    // We only need to do auto-adjust once per session, after that website seems to remember it.
    if (!autoAdjusted) {
      val uri = URI(driver.currentUrl)
      val settingsUri = URI(uri.scheme, uri.host, uri.path, "settings")

      getAndWait(settingsUri.toString())
      driver.findElementByCssSelector("#fh").clear()
      driver.findElementByCssSelector("#fh").sendKeys("25")
      val width = driver.findElementByCssSelector("#zoxval")
      width.clear()
      width.sendKeys("2100")
      val height = driver.findElementByCssSelector("#zoyval")
      height.clear()
      height.sendKeys("2970")

      val bgColour = driver.findElementByCssSelector("#bgcolor")
      bgColour.clear()
      bgColour.sendKeys("#FFFFFF")

      val fgColour = driver.findElementByCssSelector("#fgcolor")
      fgColour.clear()
      fgColour.sendKeys("#000000")

      val applyBtn =
        driver.findElementByCssSelector("#readframe > div:nth-child(6) > div:nth-child(9) > div:nth-child(20) > div.bluebtn2.f16")
      scrollIntoView(driver, applyBtn)

      applyBtn.click()
      blockUntilLoaded(wait)

      autoAdjusted = true
    }

    getAndWait(fragmentUrl)

    logger.info("Waiting for 2s for page to settle...")
    Thread.sleep(2000)

    scrollBackToStartOfFragment()

    var previousScreenshot: ByteArray? = null
    while (true) {
      for (j in 0 until 10) {
        try {
          val activeCanvas = driver.findElementByCssSelector("canvas[style='display: inline;']")
          scrollIntoView(driver, activeCanvas)
          break
        } catch (e: NoSuchElementException) {
        }
      }

      val screenshot = getScreenshot() //driver.getScreenshotAs(OutputType.BYTES)

      if (Arrays.equals(screenshot, previousScreenshot)) {
        logger.info("Equal screenshots (screenshot.size=${screenshot?.size}, previousScreenshot.size=${previousScreenshot?.size})")
        break
      } else {
        previousScreenshot = screenshot
      }
      writeScreenshot(screenshot)

      // Moves to the next page
      val bpage = driver.executeScript("return bpage(1,1);")
      logger.info("Moved to the next page, bpage(1,1) = $bpage")
      Thread.sleep(500)
    }
  }

  fun getFragments() {
    val fragmentsUrl = storyUrl + "#fragments"
    getAndWait(fragmentsUrl)
    // How to list all fragments?
    val buttonsSelector = "input.bluesmbtn[type='button']"
    val inputs = driver.findElementsByCssSelector(buttonsSelector)

    for (i in 0 until inputs.size) {
      // Repeating it because all inputs will become stale upon page
      // reload and we'll have to refreshn it anyway.
      getAndWait(fragmentsUrl)
      // How to list all fragments?
      val inputs = driver.findElementsByCssSelector(buttonsSelector)
      val button = inputs[i]

      if (i > 0) {
        // Making sure button is visible and can receive click event.
        driver.executeScript("arguments[0].scrollIntoView(true);", inputs[i - 1])
      }

      logger.info("Clicking button: $button with text ${button.text}")

      button.click()
      blockUntilLoaded(wait)
      logger.info("Current URLL: ${driver.currentUrl}")

      readFragment()
    }
  }

  fun downloadStory(): Boolean {
    if (!performLogin()) {
      logger.severe("Failed to log in, stopping downloader")
      return false
    }

    try {
      driver.findElementByCssSelector("#cookie > div.rht.flex > div").click()
      logger.info("Cookies accepted")
    } catch (e: NoSuchElementException) {
    }

    getFragments()

    return true
  }


  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val url = args[0]
      val props = Properties()
      props.load(FileInputStream("login.properties"))

      val chromeDriverPath = props.getProperty("chromedriver")

      val z = Zelluloza(
        url,
        props.getProperty("login"),
        props.getProperty("zpassword"),
        chromeDriverPath,
        props.getProperty("basedir")
      )
      z.downloadStory()
    }
  }
}