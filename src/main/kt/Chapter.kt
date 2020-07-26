import java.awt.image.BufferedImage

class Chapter(val title: String) {
    var text: String = ""

    private val images = mutableMapOf<String, BufferedImage>()
    internal fun addImage(fileName: String, data: BufferedImage) {
        images.putIfAbsent(fileName, data)
    }
}