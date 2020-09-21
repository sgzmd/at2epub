import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class Zelluloza {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val url = args[0]
      val props = Properties()
      props.load(FileInputStream("login.properties"))

      val chromeDriverPath = props.getProperty("chromedriver")

      System.setProperty("webdriver.chrome.driver", chromeDriverPath)
      val options = ChromeOptions()
      options.addArguments("--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors")
      lateinit var driver: ChromeDriver
      try {
        driver = ChromeDriver(options)
        driver.get(url)
        Thread.sleep(1000)
        var screenshotId = 1
        var previousScreenshot: ByteArray? = null

        while (true) {
          val screenshot = driver.getScreenshotAs(OutputType.BYTES)
          if (Arrays.equals(screenshot, previousScreenshot)) {

            try {
              // Next fragment
              driver.findElement(By.xpath("/html/body/div[3]/div[1]/div/div[1]/div[1]/div/div[3]/div[1]/div")).click()
            } catch (e: Exception) {
              break
            }
          } else {
            previousScreenshot = screenshot
          }
          val bos = BufferedOutputStream(FileOutputStream("result_$screenshotId.png"))
          bos.write(screenshot)
          bos.close()

          try {
            driver.findElement(By.xpath("//*[@id=\"cookie\"]/div[2]/div")).click()
          } catch (e: Exception) {
          }

//          driver.executeScript("window.scrollTo(0, document.body.scrollHeight)")
          driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN)
          Thread.sleep(10)
          screenshotId++
        }
      } finally {
        driver.close()
      }
    }
  }
}