import java.awt.image.BufferedImage

class Chapter(val title: String) {
    var text: String = ""

    internal val images = mutableMapOf<String, BufferedImage>()
        get() = field

    internal fun addImage(fileName: String, data: BufferedImage) {
        images.putIfAbsent(fileName, data)
    }
}