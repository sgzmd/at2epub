import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.FileInputStream
import java.util.*

internal class Downloader : CliktCommand() {
    val url: String by option(help = "First URL of the story in reading mode").default("")
    val config: String by option(help = "Config file to use").default("login.properties")
    val format: String by option(help = "Format to use, html/epub").default("html")
    val outputFile: String by option(help = "Output file name").default("")
    override fun run() {
        val props = Properties()
        props.load(FileInputStream(config))
        val downloader = StoryDownloader(props)
        assert(downloader.downloadStory(url) > 0)
        if (format == "html") {
            downloader.writeFiles()
        } else if (format == "epub") {
            downloader.writeEpub(outputFile)
        }
        downloader.closeWebdriver()
    }
}

class ATDownloader {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = Downloader().main(args)
    }
}