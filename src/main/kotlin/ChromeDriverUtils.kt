import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.util.logging.Logger

object ChromeDriverUtils {
  private val logger = Logger.getLogger("ChromeDriverUtils")

  fun checkPageIsLoaded(driver: RemoteWebDriver): Boolean {
    val res = driver.executeScript("return document.readyState")
    val loaded = res.equals("complete")
    logger.info("Checking page is loaded: $loaded")
    return loaded
  }

  fun blockUntilLoaded(wait: WebDriverWait) {
    wait.until {
      checkPageIsLoaded(it as RemoteWebDriver)
    }
  }

  fun scrollIntoView(driver: RemoteWebDriver, element: WebElement) {
    driver.executeScript("arguments[0].scrollIntoView(true);", element)
  }
}