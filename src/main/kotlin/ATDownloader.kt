import java.io.FileInputStream
import java.util.*

class ATDownloader {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val url = args[0]
            val props = Properties()
            props.load(FileInputStream("login.properties"))
            val downloader = StoryDownloader(props)
            assert(downloader.downloadStory(url) > 0)
            downloader.writeFiles()
        }
    }
}