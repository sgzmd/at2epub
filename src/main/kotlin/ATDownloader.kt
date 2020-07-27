import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.FileInputStream
import java.util.*

internal class Downloader : CliktCommand() {
    val url: String by option(help = "First URL of the story in reading mode").default("")
    val config: String by option(help = "Config file to use").default("login.properties")
    override fun run() {
        val props = Properties()
        props.load(FileInputStream(config))
        val downloader = StoryDownloader(props)
        assert(downloader.downloadStory(url) > 0)
        downloader.writeFiles()
    }
}

class ATDownloader {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = Downloader().main(args)
    }
}