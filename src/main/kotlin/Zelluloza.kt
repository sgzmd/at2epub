import ChromeDriverUtils.blockUntilLoaded
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.OutputType
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.FileInputStream
import java.net.URI
import java.util.*
import java.util.logging.Logger

class Zelluloza(val storyUrl: String, val userName: String, val password: String, val chromedriverPath: String) {

  private val logger = Logger.getLogger("ChromeDriverUtils")

  private var driver: ChromeDriver
  private var wait: WebDriverWait

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

  fun readFragment() {
    val uri = URI(driver.currentUrl)
    val settingsUri = URI(uri.scheme, uri.host, uri.path, "settings")
    getAndWait(settingsUri.toString())

    // Auto-adjust
    driver.findElementByCssSelector("#readframe > div:nth-child(6) > div:nth-child(9) > div.bluebtn2.f24").click()
    blockUntilLoaded(wait)

    try {
      driver.findElementByCssSelector("#cookie > div.rht.flex > div").click()
      logger.info("Cookies accepted")
    } catch (e: NoSuchElementException) {
    }

    val screenshot = driver.getScreenshotAs(OutputType.BYTES)
    logger.info("Got a screenshot, ${screenshot.size} bytes.")
  }

  fun getFragments() {
    val fragmentsUrl = storyUrl + "#fragments"
    getAndWait(fragmentsUrl)
    // How to list all fragments?
    val inputs = driver.findElementsByCssSelector("ul.g0 > li > div > div > div > input.bluesmbtn")

    for (i in 0..inputs.size - 1) {
      // Repeating it because all inputs will become stale upon page
      // reload and we'll have to refreshn it anyway.
      getAndWait(fragmentsUrl)
      // How to list all fragments?
      val inputs = driver.findElementsByCssSelector("ul.g0 > li > div > div > div > input")
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

      val z = Zelluloza(url, props.getProperty("login"), props.getProperty("zpassword"), chromeDriverPath)
      z.downloadStory()

//      System.setProperty("webdriver.chrome.driver", chromeDriverPath)
//      val options = ChromeOptions()
//      options.addArguments("--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors")
//      lateinit var driver: ChromeDriver
//      try {
//        driver = ChromeDriver(options)
//        driver.get(url)
//        Thread.sleep(1000)
//        var screenshotId = 1
//        var previousScreenshot: ByteArray? = null
//
//        while (true) {
//          val screenshot = driver.getScreenshotAs(OutputType.BYTES)
//          if (Arrays.equals(screenshot, previousScreenshot)) {
//
//            try {
//              // Next fragment
//              driver.findElement(By.xpath("/html/body/div[3]/div[1]/div/div[1]/div[1]/div/div[3]/div[1]/div"))
//                .click()
//            } catch (e: Exception) {
//              break
//            }
//          } else {
//            previousScreenshot = screenshot
//          }
//          val bos = BufferedOutputStream(FileOutputStream("result_$screenshotId.png"))
//          bos.write(screenshot)
//          bos.close()
//
//          try {
//            driver.findElement(By.xpath("//*[@id=\"cookie\"]/div[2]/div")).click()
//          } catch (e: Exception) {
//          }
//
////          driver.executeScript("window.scrollTo(0, document.body.scrollHeight)")
//          driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN)
//          Thread.sleep(10)
//          screenshotId++
//        }
//      } finally {
//        driver.close()
//      }
//    }
    }
  }
}