import java.io.File

private sealed class Element(val value: String) {

    abstract fun getKeyType() : KeyType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Element

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString() = value

    class Id(value: String = "Unknown") : Element(value) {
        override fun getKeyType(): KeyType = KeyType.ID
    }

    class Artist(value: String = "Unknown") : Element(value) {
        override fun getKeyType(): KeyType = KeyType.ARTIST
    }

    class Name(value: String = "Unknown") : Element(value) {
        override fun getKeyType(): KeyType = KeyType.NAME
    }

    companion object {
        operator fun invoke(value: String): Element? {
            return when {
                value.contains(KeyType.ID.key) -> Element.Id(extractStringValue(value))
                value.contains(KeyType.ARTIST.key) -> Element.Artist(extractStringValue(value))
                value.contains(KeyType.NAME.key) -> Element.Name(extractStringValue(value))
                else -> null
            }
        }

        private val elementValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

        private fun extractStringValue(it: String) = it.replace(elementValueRegex, "$2").replace("&#38;", "&")
    }
}

private data class Track(val id: Element.Id = Element.Id(), val artist: Element.Artist = Element.Artist(), val name: Element.Name = Element.Name()) {
    override fun toString(): String = "$artist - $name"
}

private data class Playlist(val name: Element.Name, val tracks: List<Track>) {
    override fun toString() : String = "\n\n==========\n$name:\n${tracks.joinToString("\n")}"
}

private enum class KeyType(val key: String) {
    ID("<key>Track ID</key>"),
    ARTIST("<key>Artist</key>"),
    NAME("<key>Name</key>")
}

fun main(args: Array<String>) {
    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it.extension.equals("xml", true) }

    files.forEach { println(createPlaylist(it.readLines())) }
}

private fun createPlaylist(lines: List<String>): Playlist {
    val data = lines.mapNotNull { Element(it) }
            .groupBy { it.getKeyType() }

    return with(data) {
        val ids = getValue(KeyType.ID)
        val names = getValue(KeyType.NAME)
        val entries = getValue(KeyType.ARTIST)
                .zip(names) { it, other -> Pair(it as Element.Artist, other  as Element.Name) }
                .zip(ids) { (first, second), other -> Track(other as Element.Id, first, second) }
                .sortedWith(compareBy {ids.indexOf(it.id)})

        Playlist(names.last() as Element.Name, entries)
    }
}