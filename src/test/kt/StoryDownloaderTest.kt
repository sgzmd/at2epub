import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileInputStream
import java.util.*
import kotlin.test.assertTrue

internal class StoryDownloaderTest {
    var storyDownloader: StoryDownloader? = null

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        val props = Properties()
        props.load(FileInputStream("login.properties"))
        storyDownloader = StoryDownloader(props)
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        storyDownloader?.closeWebdriver()
    }

    @org.junit.jupiter.api.Test
    fun ensureLogin() {
        assertTrue(storyDownloader?.ensureLogin()!!)
    }

    @Test
    fun downloadChapter() {
        storyDownloader?.ensureLogin()
        storyDownloader?.driver?.get("https://author.today/reader/45969/361004")
        val chapter = storyDownloader?.getNextChapter()

        assertEquals("Часть I. Глава 1", chapter?.title)
    }

    @Test
    fun downloadStory() {
        val numChapters = storyDownloader?.downloadStory("https://author.today/reader/45969/361004")
        assertEquals(27, numChapters)
    }
}